/*
 *************************************************************************
 * Copyright 2013 DFG SPP 1593 (http://dfg-spp1593.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************************************************************
 */

package org.cocome.cloud.logic.webservice.plantservice;

import org.cocome.cloud.logic.stub.IEnterpriseManager;
import org.cocome.cloud.logic.stub.IPlantManager;
import org.cocome.test.TestConfig;
import org.cocome.test.TestUtils;
import org.cocome.test.WSTestUtils;
import org.cocome.tradingsystem.inventory.application.plant.PlantTO;
import org.cocome.tradingsystem.inventory.application.plant.expression.ConditionalExpressionInfo;
import org.cocome.tradingsystem.inventory.application.plant.expression.MarkupInfo;
import org.cocome.tradingsystem.inventory.application.plant.iface.PUCImporter;
import org.cocome.tradingsystem.inventory.application.plant.iface.ppu.doub.FMU;
import org.cocome.tradingsystem.inventory.application.plant.iface.ppu.doub.XPPU;
import org.cocome.tradingsystem.inventory.application.plant.parameter.BooleanParameterTO;
import org.cocome.tradingsystem.inventory.application.plant.parameter.ParameterValueTO;
import org.cocome.tradingsystem.inventory.application.plant.productionunit.ProductionUnitClassTO;
import org.cocome.tradingsystem.inventory.application.plant.productionunit.ProductionUnitOperationTO;
import org.cocome.tradingsystem.inventory.application.plant.productionunit.ProductionUnitTO;
import org.cocome.tradingsystem.inventory.application.plant.recipe.EntryPointTO;
import org.cocome.tradingsystem.inventory.application.plant.recipe.PlantOperationOrderEntryTO;
import org.cocome.tradingsystem.inventory.application.plant.recipe.PlantOperationOrderTO;
import org.cocome.tradingsystem.inventory.application.plant.recipe.PlantOperationTO;
import org.cocome.tradingsystem.inventory.application.store.EnterpriseTO;
import org.cocome.tradingsystem.inventory.data.plant.parameter.IBooleanParameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlantManagerIT {

    private static IEnterpriseManager em = WSTestUtils.createJaxWsClient(IEnterpriseManager.class,
            TestConfig.getEnterpriseServiceWSDL());
    private static IPlantManager pm = WSTestUtils.createJaxWsClient(IPlantManager.class,
            TestConfig.getPlantManagerWSDL());

    @Test
    public void testCRUDForProductionUnitClass() throws Exception {
        final EnterpriseTO enterprise = WSTestUtils.createEnterprise(em);
        final PlantTO plant = WSTestUtils.createPlant(enterprise, em);

        final ProductionUnitClassTO puc = new ProductionUnitClassTO();
        puc.setName("PUC1");
        puc.setPlant(plant);
        puc.setId(pm.createProductionUnitClass(puc));

        final List<ProductionUnitClassTO> pucs = pm.queryProductionUnitClassesByPlantID(plant.getId());
        Assert.assertNotNull(pucs);
        Assert.assertFalse(pucs.isEmpty());

        final ProductionUnitClassTO singleInstance = pm.queryProductionUnitClassByID(pucs.get(0).getId());
        Assert.assertNotNull(singleInstance);
        Assert.assertEquals(puc.getId(), singleInstance.getId());
        Assert.assertEquals(puc.getName(), singleInstance.getName());
        for (final ProductionUnitClassTO instance : pucs) {
            pm.deleteProductionUnitClass(instance);
        }
        em.deletePlant(plant);
        em.deleteEnterprise(enterprise);
    }

    @Test
    public void testCRUDForProductionUnitOperation() throws Exception {
        final EnterpriseTO enterprise = WSTestUtils.createEnterprise(em);
        final PlantTO plant = WSTestUtils.createPlant(enterprise, em);

        final ProductionUnitClassTO puc = new ProductionUnitClassTO();
        puc.setName("PUC1");
        puc.setPlant(plant);
        puc.setId(pm.createProductionUnitClass(puc));

        final ProductionUnitOperationTO operation1 = new ProductionUnitOperationTO();
        operation1.setOperationId("__OP1");
        operation1.setName("Name_of_op1");
        operation1.setProductionUnitClass(puc);
        operation1.setExecutionDurationInMillis(10);
        operation1.setId(pm.createProductionUnitOperation(operation1));

        final ProductionUnitOperationTO operation2 = new ProductionUnitOperationTO();
        operation2.setOperationId("__OP2");
        operation2.setName("Name_of_op2");
        operation2.setProductionUnitClass(puc);
        operation2.setExecutionDurationInMillis(10);
        operation2.setId(pm.createProductionUnitOperation(operation2));

        final List<ProductionUnitOperationTO> operations =
                pm.queryProductionUnitOperationsByProductionUnitClassID(puc.getId());
        Assert.assertNotNull(operations);
        Assert.assertFalse(operations.isEmpty());

        final ProductionUnitOperationTO singleInstance =
                pm.queryProductionUnitOperationByID(operation1.getId());
        Assert.assertNotNull(singleInstance);
        Assert.assertEquals(operation1.getId(), singleInstance.getId());
        Assert.assertEquals(operation1.getName(), singleInstance.getName());
        Assert.assertEquals(operation1.getOperationId(), singleInstance.getOperationId());
        for (final ProductionUnitOperationTO instance : operations) {
            pm.deleteProductionUnitOperation(instance);
        }
        Assert.assertTrue(pm.queryProductionUnitOperationsByProductionUnitClassID(puc.getId()).isEmpty());
        pm.deleteProductionUnitClass(puc);
        em.deletePlant(plant);
        em.deleteEnterprise(enterprise);
    }

    /*
     * Can only be executed when xPPU device is running at the IP address below
     */
    @Test
    public void testPUCImport() throws Exception {
        if (TestUtils.REAL_XPPU_ENDPOINT.isEmpty()) {
            return;
        }
        final EnterpriseTO enterprise = WSTestUtils.createEnterprise(em);
        final PlantTO plant = WSTestUtils.createPlant(enterprise, em);

        final long pucId = pm.importProductionUnitClass("Test", TestUtils.REAL_XPPU_ENDPOINT, plant);

        Assert.assertTrue(pucId != 0);
    }

    @Test
    public void testOrderPlantOperation() throws Exception {
        final EnterpriseTO enterprise = WSTestUtils.createEnterprise(em);
        final PlantTO plant = WSTestUtils.createPlant(enterprise, em);

        /* Environmental setup */

        final PUCImporter xppu;
        if(TestUtils.REAL_XPPU_ENDPOINT.isEmpty()) {
            xppu = new PUCImporter("xPPU", XPPU.values(), plant, pm);
        } else {
            xppu = new PUCImporter("xPPU", plant, pm);
        }
        final PUCImporter fmu = new PUCImporter("FMU", FMU.values(), plant, pm);

        /* Production Units */

        final ProductionUnitTO xppu1 = new ProductionUnitTO();
        xppu1.setPlant(plant);
        xppu1.setProductionUnitClass(xppu.getProductionUnitClass());
        xppu1.setDouble(true);
        xppu1.setInterfaceUrl(TestUtils.REAL_XPPU_ENDPOINT);
        xppu1.setLocation("Some Place 1");
        xppu1.setId(pm.createProductionUnit(xppu1));

        final ProductionUnitTO xppu2 = new ProductionUnitTO();
        xppu2.setPlant(plant);
        xppu2.setProductionUnitClass(xppu.getProductionUnitClass());
        xppu2.setDouble(true);
        xppu2.setInterfaceUrl("dummy2.org");
        xppu2.setLocation("Some Place 2");
        xppu2.setId(pm.createProductionUnit(xppu2));

        final ProductionUnitTO fmu3 = new ProductionUnitTO();
        fmu3.setPlant(plant);
        fmu3.setProductionUnitClass(fmu.getProductionUnitClass());
        fmu3.setDouble(true);
        fmu3.setInterfaceUrl("dummy2.org");
        fmu3.setLocation("Some Place 3");
        fmu3.setId(pm.createProductionUnit(fmu3));

        /* Plant Operations */

        final PlantOperationTO operation = new PlantOperationTO();
        operation.setName("Produce Yogurt");
        operation.setPlant(plant);
        operation.setMarkup(new MarkupInfo(
                Arrays.asList(
                        xppu.getOperation(XPPU.Crane_ACT_Init),
                        xppu.getOperation(XPPU.Stack_ACT_Init),
                        xppu.getOperation(XPPU.Stamp_ACT_Init),
                        xppu.getOperation(XPPU.Stack_ACT_ProvideWP),
                        xppu.getOperation(XPPU.Crane_ACT_PickUpWP),
                        new ConditionalExpressionInfo(
                                "Organic",
                                IBooleanParameter.TRUE_VALUE,
                                Collections.singletonList(
                                        xppu.getOperation(XPPU.Crane_ACT_TurnToStamp)),
                                Collections.singletonList(
                                        xppu.getOperation(XPPU.Crane_ACT_TurnToConveyor)
                                )),
                        xppu.getOperation(XPPU.Crane_ACT_PutDownWP),
                        fmu.getOperation(FMU.Silo0_ACT_Init),
                        fmu.getOperation(FMU.Silo1_ACT_Init),
                        fmu.getOperation(FMU.Silo2_ACT_Init)
                )));
        operation.setId(em.createPlantOperation(operation));

        final EntryPointTO e = new EntryPointTO();
        e.setName("ISO 12345 Cargo");
        e.setOperation(operation);
        e.setDirection(EntryPointTO.DirectionTO.OUTPUT);
        e.setId(em.createEntryPoint(e));

        final BooleanParameterTO param = new BooleanParameterTO();
        param.setCategory("Yogurt Preparation");
        param.setOperation(operation);
        param.setName("Organic");
        param.setId(em.createBooleanParameter(param));

        /* Order creation */

        final PlantOperationOrderTO operationOrder = new PlantOperationOrderTO();
        operationOrder.setEnterprise(enterprise);
        operationOrder.setPlant(plant);

        final ParameterValueTO paramValue = new ParameterValueTO(IBooleanParameter.FALSE_VALUE, param);

        final PlantOperationOrderEntryTO entry = new PlantOperationOrderEntryTO();
        entry.setPlantOperation(operation);
        entry.setAmount(2);
        entry.setParameterValues(Collections.singletonList(paramValue));

        operationOrder.setOrderEntries(Collections.singletonList(entry));
        operationOrder.setId(pm.orderOperation(operationOrder));
        Assert.assertTrue(operationOrder.getId() > 0);
    }
}