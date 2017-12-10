package org.cocome.cloud.web.frontend.plant;

import org.cocome.cloud.logic.stub.NotInDatabaseException_Exception;
import org.cocome.cloud.web.data.plantdata.PlantViewData;
import org.cocome.cloud.web.data.plantdata.ProductionUnitClassDAO;
import org.cocome.cloud.web.data.plantdata.ProductionUnitClassViewData;
import org.cocome.cloud.web.frontend.AbstractView;
import org.cocome.cloud.web.frontend.navigation.NavigationElements;
import org.cocome.cloud.web.frontend.util.Messages;
import org.cocome.tradingsystem.inventory.application.plant.productionunit.ProductionUnitClassTO;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

/**
 * Holds information about the currently active plant.
 *
 * @author Rudolf Biczok
 */
@Named
@ViewScoped
public class ProductionUnitClassView extends AbstractView<ProductionUnitClassTO> {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProductionUnitClassDAO productionUnitClassDAO;

    @Inject
    private PlantInformation plantInformation;

    private ProductionUnitClassViewData newInstance;

    @PostConstruct
    public void createNewInstance() {
        this.newInstance = new ProductionUnitClassViewData(new ProductionUnitClassTO());
        this.newInstance.getData().setPlant(plantInformation.getActivePlant().getData());
    }

    public String importPUC(
            @NotNull String name,
            @NotNull String interfaceUrl,
            @NotNull PlantViewData plant) throws NotInDatabaseException_Exception {

        return processFacesAction(() -> productionUnitClassDAO.importPUC(name, interfaceUrl, plant),
                Messages.get("message.import.success", Messages.get("puc.short.text")),
                Messages.get("message.import.failed", Messages.get("puc.short.text")),
                NavigationElements.PLANT_PUC);
    }

    public ProductionUnitClassViewData getNewInstance() {
        return newInstance;
    }

    @Override
    protected ProductionUnitClassDAO getDAO() {
        return this.productionUnitClassDAO;
    }

    @Override
    protected NavigationElements getNextNavigationElement() {
        return NavigationElements.PLANT_PUC;
    }

    @Override
    protected String getObjectName() {
        return Messages.get("puc.short.text");
    }
}
