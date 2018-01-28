package org.cocome.tradingsystem.inventory.data.enterprise;

import org.cocome.tradingsystem.inventory.data.persistence.CloudPersistenceContext;
import org.cocome.tradingsystem.inventory.data.persistence.IPersistenceContext;
import org.cocome.tradingsystem.inventory.data.plant.IPlant;
import org.cocome.tradingsystem.inventory.data.plant.IPlantQuery;
import org.cocome.tradingsystem.inventory.data.plant.Plant;
import org.cocome.tradingsystem.inventory.data.plant.PlantDatatypesFactory;
import org.cocome.tradingsystem.inventory.data.plant.parameter.*;
import org.cocome.tradingsystem.inventory.data.plant.productionunit.IProductionUnitClass;
import org.cocome.tradingsystem.inventory.data.plant.productionunit.IProductionUnitOperation;
import org.cocome.tradingsystem.inventory.data.plant.productionunit.ProductionUnitClass;
import org.cocome.tradingsystem.inventory.data.plant.productionunit.ProductionUnitOperation;
import org.cocome.tradingsystem.inventory.data.plant.recipe.EnterprisePlantQueryProvider;
import org.cocome.tradingsystem.inventory.data.plant.recipe.IRecipe;
import org.cocome.tradingsystem.inventory.data.plant.recipe.Recipe;
import org.cocome.tradingsystem.inventory.data.store.EnterpriseStoreQueryProvider;
import org.cocome.tradingsystem.inventory.data.store.StoreDatatypesFactory;
import org.cocome.tradingsystem.inventory.data.usermanager.UsermanagerDatatypesFactory;
import org.cocome.tradingsystem.remote.access.connection.CSVBackendConnection;
import org.cocome.tradingsystem.remote.access.connection.GetXMLFromBackend;
import org.cocome.tradingsystem.remote.access.parsing.CSVHelper;
import org.cocome.tradingsystem.util.Configuration;
import org.cocome.tradingsystem.util.exception.NotInDatabaseException;
import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.jglue.cdiunit.InRequestScope;
import org.jglue.cdiunit.ejb.SupportEjb;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.CreateException;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;

/**
 * Test unit for query providers and {@link CloudPersistenceContext}
 *
 * @author Rudolf Biczok
 */
@RunWith(CdiRunner.class)
@SupportEjb
@AdditionalClasses({
        Configuration.class,
        EnterprisePlantQueryProvider.class,
        CloudPersistenceContext.class,
        CSVBackendConnection.class,
        EnterpriseQueryProvider.class,
        CSVHelper.class,
        GetXMLFromBackend.class,
        EnterpriseStoreQueryProvider.class,
        PlantDatatypesFactory.class,
        EnterpriseDatatypesFactory.class,
        StoreDatatypesFactory.class,
        BooleanParameter.class,
        NominalParameter.class,
        UsermanagerDatatypesFactory.class})
public class QueryProviderAndPersistenceIT {

    @Inject
    private IPersistenceContext persistenceContext;

    @Inject
    private IPlantQuery plantQuery;

    @Inject
    private IEnterpriseQuery enterpriseQuery;

    @Test
    public void createAndDeleteNominalParameter() throws Exception {
        final ICustomProduct prod = new CustomProduct();
        prod.setBarcode(new Date().getTime());
        prod.setName("Fancy Product");
        prod.setPurchasePrice(100);
        persistenceContext.createEntity(prod);

        final ITradingEnterprise enterprise = new TradingEnterprise();
        enterprise.setName(randomName());
        persistenceContext.createEntity(enterprise);

        final IRecipe recipe = new Recipe();
        recipe.setName("Produce Fancy Product");
        recipe.setEnterprise(enterprise);
        recipe.setEnterpriseId(enterprise.getId());
        recipe.setCustomProduct(prod);
        recipe.setCustomProductBarcode(prod.getBarcode());
        persistenceContext.createEntity(recipe);

        final INominalParameter param = new NominalParameter();
        param.setOperation(recipe);
        param.setOperationId(recipe.getId());
        param.setOptions(new HashSet<>(Arrays.asList("Op1", "Op2")));
        param.setCategory("SomeCategory");
        param.setName("SomeName");
        persistenceContext.createEntity(param);

        persistenceContext.deleteEntity(param);
        persistenceContext.deleteEntity(recipe);
        persistenceContext.deleteEntity(prod);
        persistenceContext.deleteEntity(enterprise);
    }

    private String randomName() {
        return String.format("%s-%s", "Test Enterprise", UUID.randomUUID().toString());
    }

    @InRequestScope
    @Test
    public void testCRUDForProductionUnitOperation() throws Exception {
        final ITradingEnterprise enterprise = getOrCreateEnterprise();
        final IPlant plant = getOrCreatePlant(enterprise);

        final IProductionUnitClass puc = new ProductionUnitClass();
        puc.setName("PUC1");
        puc.setPlantId(plant.getId());
        persistenceContext.createEntity(puc);

        final IProductionUnitOperation operation1 = new ProductionUnitOperation();
        operation1.setOperationId("__OP1");
        operation1.setName("Name_of_op1");
        operation1.setProductionUnitClassId(puc.getId());
        operation1.setExecutionDurationInMillis(10);
        persistenceContext.createEntity(operation1);

        final IProductionUnitOperation operation2 = new ProductionUnitOperation();
        operation2.setOperationId("__OP2");
        operation2.setName("Name_of_op2");
        operation2.setProductionUnitClassId(puc.getId());
        operation2.setExecutionDurationInMillis(10);
        persistenceContext.createEntity(operation2);

        final Set<String> queriedInstances = plantQuery
                .queryProductionUnitOperationsByProductionUnitClassId(puc.getId())
                .stream().map(IProductionUnitOperation::getOperationId)
                .collect(Collectors.toSet());

        Assert.assertEquals(2, queriedInstances.size());
        Assert.assertThat(queriedInstances, hasItems("__OP1", "__OP2"));

        persistenceContext.deleteEntity(operation1);
        persistenceContext.deleteEntity(operation2);
        persistenceContext.deleteEntity(puc);
        persistenceContext.deleteEntity(plant);
        persistenceContext.deleteEntity(enterprise);
    }

    @InRequestScope
    @Test
    public void queryParametersByCustomProductID() throws Exception {
        final ICustomProduct prod = new CustomProduct();
        prod.setBarcode(new Date().getTime());
        prod.setName("Fancy Product");
        prod.setPurchasePrice(100);
        persistenceContext.createEntity(prod);

        final ITradingEnterprise enterprise = new TradingEnterprise();
        enterprise.setName(randomName());
        persistenceContext.createEntity(enterprise);

        final IRecipe recipe = new Recipe();
        recipe.setName("Produce Fancy Product");
        recipe.setEnterprise(enterprise);
        recipe.setEnterpriseId(enterprise.getId());
        recipe.setCustomProduct(prod);
        recipe.setCustomProductBarcode(prod.getBarcode());
        persistenceContext.createEntity(recipe);

        final IBooleanParameter param = new BooleanParameter();
        param.setOperation(recipe);
        param.setOperationId(recipe.getId());
        param.setCategory("SomeCategory");
        param.setName("SomeName2");
        persistenceContext.createEntity(param);

        final INominalParameter param2 = new NominalParameter();
        param2.setOperation(recipe);
        param2.setOperationId(recipe.getId());
        param2.setOptions(new HashSet<>(Arrays.asList("Op1", "Op2")));
        param2.setCategory("SomeCategory");
        param2.setName("SomeName");
        persistenceContext.createEntity(param2);

        final List<IParameter> params =
                new ArrayList<>(enterpriseQuery.queryParametersByRecipeOperationId(recipe.getId()));
        Assert.assertEquals(2, params.size());
        final List<Class<?>> paramClasses = params
                .stream()
                .map(Object::getClass)
                .collect(Collectors.toList());

        Assert.assertTrue(paramClasses.contains(BooleanParameter.class));
        Assert.assertTrue(paramClasses.contains(NominalParameter.class));

        persistenceContext.deleteEntity(param);
        persistenceContext.deleteEntity(param2);
        persistenceContext.deleteEntity(recipe);
        persistenceContext.deleteEntity(prod);
        persistenceContext.deleteEntity(enterprise);
    }

    private ITradingEnterprise getOrCreateEnterprise() throws CreateException {
        final String enterpriseName = String.format("Enterprise-%s", UUID.randomUUID().toString());
        final ITradingEnterprise enterprise;
        try {
            enterprise = enterpriseQuery.queryEnterpriseByName(enterpriseName);
        } catch (NotInDatabaseException e) {
            final ITradingEnterprise newEnterprise = new TradingEnterprise();
            newEnterprise.setName(enterpriseName);
            persistenceContext.createEntity(newEnterprise);
            return newEnterprise;
        }
        return enterprise;
    }

    private IPlant getOrCreatePlant(final ITradingEnterprise enterprise)
            throws CreateException {
        final IPlant plant = new Plant();
        plant.setName("Plant1");
        plant.setEnterpriseId(enterprise.getId());
        persistenceContext.createEntity(plant);
        return plant;
    }
}