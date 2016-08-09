package org.cocome.cloud.web.frontend.cashdesk;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.cocome.cloud.logic.stub.IllegalCashDeskStateException_Exception;
import org.cocome.cloud.logic.stub.IllegalInputException_Exception;
import org.cocome.cloud.logic.stub.NoSuchProductException_Exception;
import org.cocome.cloud.logic.stub.NotInDatabaseException_Exception;
import org.cocome.cloud.logic.stub.ProductOutOfStockException_Exception;
import org.cocome.cloud.logic.stub.UnhandledException_Exception;
import org.cocome.cloud.web.backend.cashdesk.ICashDeskQuery;
import org.cocome.cloud.web.data.cashdesk.ICashDesk;
import org.cocome.cloud.web.frontend.navigation.NavigationElements;
import org.cocome.cloud.web.frontend.store.IStoreInformation;
import org.cocome.cloud.web.util.Messages;

@ViewScoped
@ManagedBean
public class CashDeskView implements Serializable {
	private static final long serialVersionUID = -2512543291563857980L;

	private static final String[] EMPTY_OUTPUT = {};

	@Inject
	IStoreInformation storeInformation;

	@Inject
	ICashDeskQuery cashDeskQuery;

	@Inject
	ICashDesk cashDesk;

	public String submitCashDeskName() {
		cashDesk.setCashDeskNameNeeded(false);
		updateExpressMode();
		return resetSale();
	}

	private void updateDisplayAndPrinter() {
		updateDisplayMessage();
		updatePrinterOutput();
	}

	private String getSalePageRedirectOutcome() {
		return NavigationElements.START_SALE.getNavigationOutcome();
	}

	public void setCashDeskName(String cashDeskName) {
		cashDesk.setCashDeskName(cashDeskName);
	}

	public String getCashDeskName() {
		return cashDesk.getCashDeskName();
	}

	public boolean isCashDeskNameNeeded() {
		return cashDesk.isCashDeskNameNeeded();
	}

	public boolean isSaleStarted() {
		return cashDesk.isSaleStarted();
	}

	public boolean isInExpressMode() {
		return cashDesk.isInExpressMode();
	}

	public String getDisplayMessage() {
		return cashDesk.getDisplayMessage();
	}

	public String[] getPrinterOutput() {
		return cashDesk.getPrinterOutput();
	}

	public void updateExpressMode() {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();
		
		boolean expressMode = false;
		
		try {
			expressMode = cashDeskQuery.isInExpressMode(cashDeskName, storeID);
		} catch (UnhandledException_Exception | NotInDatabaseException_Exception e) {
			addFacesError(Messages.getLocalizedMessage("cashdesk.error.express.retrieve"));
		}
		
		cashDesk.setInExpressMode(expressMode);
	}

	private void addFacesError(String errorString) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR", errorString));
	}

	public void updateDisplayMessage() {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		String displayMessage = "";

		try {
			displayMessage = cashDeskQuery.getDisplayMessage(cashDeskName, storeID);
		} catch (UnhandledException_Exception | NotInDatabaseException_Exception e) {
			addFacesError(Messages.getLocalizedMessage("cashdesk.error.display.retrieve"));
		}
		
		cashDesk.setDisplayMessage(displayMessage);
	}

	public String enterCashAmount(double cashAmount) {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.enterCashAmount(cashDeskName, storeID, cashAmount);
		} catch (UnhandledException_Exception | NotInDatabaseException_Exception | IllegalCashDeskStateException_Exception e) {
			addFacesError(String.format(Messages.getLocalizedMessage("cashdesk.error.cash_pay.failed"), e.getMessage()));
		}
		
		updateDisplayAndPrinter();
		updateExpressMode();
		return getSalePageRedirectOutcome();
	}

	public String enterCardInfo(String cardInfo, int pin) {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.enterCardInfo(cashDeskName, storeID, cardInfo, pin);
		} catch (UnhandledException_Exception | IllegalCashDeskStateException_Exception
				| NotInDatabaseException_Exception e) {
			addFacesError(
					String.format(Messages.getLocalizedMessage("cashdesk.error.card_pay.failed"), e.getMessage()));
		}
		updateDisplayAndPrinter();
		updateExpressMode();
		return getSalePageRedirectOutcome();
	}

	public String startCashPayment() {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.startCashPayment(cashDeskName, storeID);
			cashDesk.setAllItemsRegistered(true);
			cashDesk.setCashPayment(true);
			cashDesk.setCardPayment(false);
		} catch (NotInDatabaseException_Exception | ProductOutOfStockException_Exception | UnhandledException_Exception
				| IllegalCashDeskStateException_Exception | IllegalInputException_Exception e) {
			addFacesError(String.format(Messages.getLocalizedMessage("cashdesk.error.start_cash_pay.failed"),
					e.getMessage()));
		}

		updateDisplayAndPrinter();

		return getSalePageRedirectOutcome();
	}

	public String startCardPayment() {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.startCreditCardPayment(cashDeskName, storeID);
			cashDesk.setAllItemsRegistered(true);
			cashDesk.setCardPayment(true);
			cashDesk.setCashPayment(false);
		} catch (NotInDatabaseException_Exception | ProductOutOfStockException_Exception | UnhandledException_Exception
				| IllegalCashDeskStateException_Exception | IllegalInputException_Exception e) {
			addFacesError(String.format(Messages.getLocalizedMessage("cashdesk.error.start_card_pay.failed"),
					e.getMessage()));
		}

		updateDisplayAndPrinter();

		return getSalePageRedirectOutcome();
	}

	public String resetSale() {
		
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.startSale(cashDeskName, storeID);
			cashDesk.setSaleStarted(true);
			cashDesk.setCashPayment(false);
			cashDesk.setCardPayment(false);
			clearBarcode();
		} catch (UnhandledException_Exception | IllegalCashDeskStateException_Exception
				| NotInDatabaseException_Exception e) {
			addFacesError(Messages.getLocalizedMessage("cashdesk.error.illegal_state.start_sale"));
		}

		
		updateDisplayAndPrinter();

		return getSalePageRedirectOutcome();
	}

	public boolean isCashPayment() {
		return cashDesk.isCashPayment();
	}

	public boolean isCardPayment() {
		return cashDesk.isCardPayment();
	}

	private void handleFailedValidationMessage(FacesContext context, UIComponent comp, String message) {
		((UIInput) comp).setValid(false);
		FacesMessage wrongInputMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR", message);
		context.addMessage(comp.getClientId(), wrongInputMessage);
	}

	public void validateCashAmount(FacesContext context, UIComponent comp, Object value) {
		String input = (String) value;
		double cashAmount;

		try {
			cashAmount = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			handleFailedValidationMessage(context, comp,
					Messages.getLocalizedMessage("cashdesk.validation.amount.failed"));
			return;
		}

		if (cashAmount < 0) {
			handleFailedValidationMessage(context, comp,
					Messages.getLocalizedMessage("cashdesk.validation.amount.failed"));
		}
	}

	public String getBarcode() {
		return cashDesk.getBarcode();
	}

	public void setBarcode(String barcode) {
		cashDesk.setBarcode(barcode);
	}

	private long convertBarcode() throws NumberFormatException {
		long barcode = Long.parseLong(cashDesk.getBarcode());
		if (barcode < 0) {
			throw new NumberFormatException("Barcode must be positive!");
		}
		return barcode;
	}

	public String scanBarcode() {
		long barcode;
		try {
			barcode = convertBarcode();
		} catch (NumberFormatException e) {
			handleFailedValidationMessage(FacesContext.getCurrentInstance(),
					FacesContext.getCurrentInstance().getViewRoot().findComponent("barcodetext"),
					Messages.getLocalizedMessage("cashdesk.validation.barcode.failed"));
			return getSalePageRedirectOutcome();
		}

		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		try {
			cashDeskQuery.enterBarcode(cashDeskName, storeID, barcode);
			updatePrinterOutput();
		} catch (UnhandledException_Exception | IllegalCashDeskStateException_Exception
				| NotInDatabaseException_Exception | NoSuchProductException_Exception
				| ProductOutOfStockException_Exception e) {
			addFacesError(String.format(Messages.getLocalizedMessage("cashdesk.barcode.scan.failed"), e.getMessage()));
		}
		return getSalePageRedirectOutcome();
	}

	public void addDigitToBarcode(char digit) {
		// TODO Perhaps use a StringBuilder for this
		cashDesk.setBarcode(cashDesk.getBarcode() + digit);
	}

	public void clearBarcode() {
		cashDesk.setBarcode("");
	}

	public void removeLastBarcodeDigit() {
		String barcode = cashDesk.getBarcode();
		cashDesk.setBarcode(barcode.substring(0, barcode.length() - 2));
	}

	public void updatePrinterOutput() {
		String cashDeskName = cashDesk.getCashDeskName();
		long storeID = storeInformation.getActiveStoreID();

		String[] printerOutput;

		try {
			printerOutput = cashDeskQuery.getPrinterOutput(cashDeskName, storeID);
		} catch (UnhandledException_Exception | NotInDatabaseException_Exception e) {
			addFacesError(
					String.format(Messages.getLocalizedMessage("cashdesk.error.printer.retrieve"), e.getMessage()));
			printerOutput = EMPTY_OUTPUT;
		}
		cashDesk.setPrinterOutput(printerOutput);
	}
}