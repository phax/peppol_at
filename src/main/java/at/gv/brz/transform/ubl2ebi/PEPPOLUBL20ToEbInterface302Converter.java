package at.gv.brz.transform.ubl2ebi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PersonType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoiceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ProfileIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionIDType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.gv.brz.transform.ubl2ebi.helper.SchemedID;
import at.gv.brz.transform.ubl2ebi.helper.TaxCategoryKey;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.ebinterface.v302.AccountType;
import com.phloc.ebinterface.v302.AddressType;
import com.phloc.ebinterface.v302.BillerType;
import com.phloc.ebinterface.v302.CurrencyType;
import com.phloc.ebinterface.v302.DetailsType;
import com.phloc.ebinterface.v302.DocumentTypeType;
import com.phloc.ebinterface.v302.InvoiceRecipientType;
import com.phloc.ebinterface.v302.InvoiceType;
import com.phloc.ebinterface.v302.ItemListType;
import com.phloc.ebinterface.v302.ItemType;
import com.phloc.ebinterface.v302.ListLineItemType;
import com.phloc.ebinterface.v302.ObjectFactory;
import com.phloc.ebinterface.v302.OrderReferenceDetailType;
import com.phloc.ebinterface.v302.OrderReferenceType;
import com.phloc.ebinterface.v302.TaxRateType;
import com.phloc.ebinterface.v302.TaxType;
import com.phloc.ebinterface.v302.UnitType;
import com.phloc.ebinterface.v302.UniversalBankTransactionType;
import com.phloc.ebinterface.v302.VATType;
import com.phloc.ubl20.codelist.EUnitOfMeasureCode20;

import eu.europa.ec.cipa.peppol.identifier.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.IPeppolPredefinedProcessIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.PredefinedProcessIdentifierManager;

/**
 * Main converter between UBL 2.0 invoice and ebInterface 3.0.2 invoice.
 * 
 * @author philip
 */
@Immutable
public final class PEPPOLUBL20ToEbInterface302Converter {
  private static final Logger s_aLogger = LoggerFactory.getLogger (PEPPOLUBL20ToEbInterface302Converter.class);
  private static final String DUMMY_VALUE = "DUMMY_VALUE";
  private static final String REGEX_BIC = "[0-9|A-Z|a-z]{8}([0-9|A-Z|a-z]{3})?";
  private static final String SUPPORTED_TAX_SCHEME_SCHEME_ID = "UN/ECE 5153";
  private static final int IBAN_MAX_LENGTH = 34;
  private static final String PAYMENT_CHANNEL_CODE_IBAN = "IBAN";
  private static final String SUPPORTED_TAX_SCHEME_ID = "VAT";
  private static final String EBI_GENERATING_SYSTEM = "UBL 2.0 to ebInterface 3.0.2 converter";

  private PEPPOLUBL20ToEbInterface302Converter () {}

  /**
   * Check if the passed UBL invoice is transformable
   * 
   * @param aUBLInvoice
   *        The UBL invoice to check
   * @return <code>null</code> in case of no error, the error message otherwise
   */
  @Nullable
  private static String _checkConsistency (final oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType aUBLInvoice) {
    // Check UBLVersionID
    final UBLVersionIDType aUBLVersionID = aUBLInvoice.getUBLVersionID ();
    if (aUBLVersionID == null)
      return "No UBLVersionID present!";
    if (!CPeppolUBL.UBL_VERSION.equals (aUBLVersionID.getValue ()))
      return "Invalid UBLVersionID value present!";

    // Check ProfileID
    final ProfileIDType aProfileID = aUBLInvoice.getProfileID ();
    if (aProfileID == null)
      return "No ProfileID present!";
    final String sProfileID = aProfileID.getValue ();
    final IPeppolPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
    if (aProcID == null)
      return "Invalid ProfileID value present!";

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false) {
      final CustomizationIDType aCustomizationID = aUBLInvoice.getCustomizationID ();
      if (aCustomizationID == null)
        return "No CustomizationID present!";
      if (!CPeppolUBL.CUSTOMIZATION_SCHEMEID.equals (aCustomizationID.getSchemeID ()))
        return "Invalid CustomizationID schemeID present!";
      final String sCustomizationID = aCustomizationID.getValue ();
      IPeppolPredefinedDocumentTypeIdentifier aMatchingDocID = null;
      for (final IPeppolPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
        if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID)) {
          // We found a match
          aMatchingDocID = aDocID;
          break;
        }
      if (aMatchingDocID == null)
        return "Invalid CustomizationID value present!";
    }

    // Invoice type code
    final InvoiceTypeCodeType aInvoiceTypeCode = aUBLInvoice.getInvoiceTypeCode ();
    if (aInvoiceTypeCode == null) {
      // None present
      s_aLogger.warn ("No InvoiceTypeCode present! Assuming " + CPeppolUBL.INVOICE_TYPE_CODE);
    }
    else {
      // If one is present, it must match
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (aInvoiceTypeCode.getValue ()))
        return "Invalid InvoiceTypeCode value present!";
    }

    // Done
    return null;
  }

  @Nonnull
  private static AddressType _convertAddress (final ObjectFactory aObjectFactory, final PartyType aUBLParty) {
    final AddressType ret = aObjectFactory.createAddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      ret.setName (aUBLPartyName.getNameValue ());
      if (aUBLParty.getPartyNameCount () > 1)
        s_aLogger.warn ("UBL invoice has multiple party names - only the first one is used!");
    }

    // Convert main address
    final oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType aUBLAddress = aUBLParty.getPostalAddress ();
    if (aUBLAddress != null) {
      ret.setStreet (StringHelper.getImplodedNonEmpty (" ",
                                                       aUBLAddress.getStreetNameValue (),
                                                       aUBLAddress.getBuildingNumberValue ()));
      ret.setPOBox (aUBLAddress.getPostboxValue ());
      ret.setTown (aUBLAddress.getCityNameValue ());
      ret.setZIP (aUBLAddress.getPostalZoneValue ());
      if (aUBLAddress.getCountry () != null)
        ret.setCountry (aUBLAddress.getCountry ().getIdentificationCodeValue ());
    }

    // Contact
    final ContactType aUBLContact = aUBLParty.getContact ();
    if (aUBLContact != null) {
      ret.setPhone (aUBLContact.getTelephoneValue ());
      ret.setEmail (aUBLContact.getElectronicMailValue ());
    }

    // Person name
    final PersonType aUBLPerson = aUBLParty.getPerson ();
    if (aUBLPerson != null) {
      ret.setContact (StringHelper.getImplodedNonEmpty (" ",
                                                        aUBLPerson.getTitleValue (),
                                                        aUBLPerson.getFirstNameValue (),
                                                        aUBLPerson.getMiddleNameValue (),
                                                        aUBLPerson.getFamilyNameValue (),
                                                        aUBLPerson.getNameSuffixValue ()));
    }

    // Check all mandatory fields
    if (ret.getName () == null)
      ret.setName (DUMMY_VALUE);
    if (ret.getStreet () == null)
      ret.setStreet (DUMMY_VALUE);
    if (ret.getTown () == null)
      ret.setTown (DUMMY_VALUE);
    if (ret.getZIP () == null)
      ret.setZIP (DUMMY_VALUE);
    if (ret.getCountry () == null)
      ret.setCountry (DUMMY_VALUE);

    return ret;
  }

  private static boolean _isSupportedTaxSchemeSchemeID (@Nullable final String sUBLTaxSchemeSchemeID) {
    return sUBLTaxSchemeSchemeID == null || sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID);
  }

  /**
   * Main conversion method to convert from UBL 2.0 to ebInterface 3.0.2
   * 
   * @param aUBLInvoice
   *        The UBL invoice to be converted
   * @return The created ebInterface 3.0.2 document
   * @throws IllegalArgumentException
   *         If the passed UBL invoice cannot be converted
   */
  @Nonnull
  public static InvoiceType convertToEbInterface (@Nonnull final oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType aUBLInvoice) {
    if (aUBLInvoice == null)
      throw new NullPointerException ("UBLInvoice");

    final ObjectFactory aObjectFactory = new ObjectFactory ();

    // Consistency check before starting the conversion
    final String sConsistencyValidationResult = _checkConsistency (aUBLInvoice);
    if (sConsistencyValidationResult != null)
      throw new IllegalArgumentException ("Consistency validation failed: " + sConsistencyValidationResult);

    // Build ebInterface invoice
    final InvoiceType aNewInvoice = aObjectFactory.createInvoiceType ();
    aNewInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aNewInvoice.setDocumentType (DocumentTypeType.INVOICE);
    aNewInvoice.setInvoiceCurrency (CurrencyType.fromValue (StringHelper.trim (aUBLInvoice.getDocumentCurrencyCode ()
                                                                                          .getValue ())));
    aNewInvoice.setInvoiceNumber (aUBLInvoice.getID ().getValue ());
    aNewInvoice.setInvoiceDate (aUBLInvoice.getIssueDate ().getValue ());

    // Biller (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLInvoice.getAccountingSupplierParty ();
      final BillerType aNewBiller = aObjectFactory.createBillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (aPartyTaxScheme.getTaxScheme ().getID ().getValue ().equals (SUPPORTED_TAX_SCHEME_ID)) {
          aNewBiller.setVATIdentificationNumber (aPartyTaxScheme.getCompanyID ().getValue ());
          break;
        }
      if (aNewBiller.getVATIdentificationNumber () == null) {
        // A VAT number must be present!
        s_aLogger.error ("Failed to get biller VAT number!");
        aNewBiller.setVATIdentificationNumber (DUMMY_VALUE);
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null) {
        // The customer's internal identifier for the supplier.
        aNewBiller.setInvoiceRecipientsBillerID (aUBLSupplier.getCustomerAssignedAccountID ().getValue ());
      }
      aNewBiller.setAddress (_convertAddress (aObjectFactory, aUBLSupplier.getParty ()));
      aNewInvoice.setBiller (aNewBiller);
    }

    // Invoice recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLInvoice.getAccountingCustomerParty ();
      final InvoiceRecipientType aNewRecipient = aObjectFactory.createInvoiceRecipientType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aPartyTaxScheme : aUBLCustomer.getParty ().getPartyTaxScheme ())
        if (aPartyTaxScheme.getTaxScheme ().getID ().getValue ().equals (SUPPORTED_TAX_SCHEME_ID)) {
          aNewRecipient.setVATIdentificationNumber (aPartyTaxScheme.getCompanyID ().getValue ());
          break;
        }
      if (aNewRecipient.getVATIdentificationNumber () == null) {
        // Mandatory field
        s_aLogger.error ("Failed to get supplier VAT number!");
        aNewRecipient.setVATIdentificationNumber (DUMMY_VALUE);
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null) {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        aNewRecipient.setBillersInvoiceRecipientID (aUBLCustomer.getSupplierAssignedAccountID ().getValue ());
      }
      else {
        // Mandatory field
        s_aLogger.error ("Failed to get supplier assigned account ID for customer!");
        aNewRecipient.setBillersInvoiceRecipientID (DUMMY_VALUE);
      }
      aNewRecipient.setAddress (_convertAddress (aObjectFactory, aUBLCustomer.getParty ()));
      aNewInvoice.setInvoiceRecipient (aNewRecipient);
    }

    // Order reference of invoice recipient
    String sOrderReferenceID;
    {
      // Get accounting area (if any)
      String sAccountingArea = null;
      if (aUBLInvoice.getAccountingCost () != null)
        sAccountingArea = aUBLInvoice.getAccountingCost ().getValue ();

      String sUBLOrderReferenceID = null;

      final oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderReferenceType aUBLOrderReference = aUBLInvoice.getOrderReference ();
      if (aUBLOrderReference != null) {
        // Use directly from order reference
        sUBLOrderReferenceID = aUBLOrderReference.getID ().getValue ();
      }
      else {
        // Check if a contract reference is present
        final List <DocumentReferenceType> aUBLContractDocumentReferences = aUBLInvoice.getContractDocumentReference ();
        if (!aUBLContractDocumentReferences.isEmpty ()) {
          // ID is mandatory
          sUBLOrderReferenceID = ContainerHelper.getFirstElement (aUBLContractDocumentReferences).getID ().getValue ();
        }
      }

      // Concatenate accounting area and main order reference for ebInterface
      // 3.x
      sOrderReferenceID = StringHelper.getConcatenatedOnDemand (sAccountingArea, ":", sUBLOrderReferenceID);
      if (StringHelper.hasNoText (sOrderReferenceID)) {
        s_aLogger.error ("Failed to get order reference ID!");
        sOrderReferenceID = DUMMY_VALUE;
      }
      else
        if (sOrderReferenceID.length () > 35) {
          s_aLogger.warn ("Order reference value '" +
                          sOrderReferenceID +
                          "' is too long. It will be cut to 35 characters.");
          sOrderReferenceID = sOrderReferenceID.substring (0, 35);
        }

      final OrderReferenceType aNewOrderReference = aObjectFactory.createOrderReferenceType ();
      aNewOrderReference.setOrderID (sOrderReferenceID);
      aNewInvoice.getInvoiceRecipient ().setOrderReference (aNewOrderReference);
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final TaxType aNewTax = aObjectFactory.createTaxType ();
    final VATType aNewVAT = aObjectFactory.createVATType ();
    {
      for (final TaxTotalType aUBLTaxTotal : aUBLInvoice.getTaxTotal ())
        for (final TaxSubtotalType aUBLSubtotal : aUBLTaxTotal.getTaxSubtotal ()) {
          // Tax category is a mandatory element
          final TaxCategoryType aUBLTaxCategory = aUBLSubtotal.getTaxCategory ();

          // Is the percentage value directly specified
          BigDecimal aUBLPercentage = aUBLTaxCategory.getPercentValue ();
          if (aUBLPercentage == null) {
            // no it is not :(
            final BigDecimal aUBLTaxAmount = aUBLSubtotal.getTaxAmountValue ();
            final BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmountValue ();
            if (aUBLTaxAmount != null && aUBLTaxableAmount != null)
              aUBLPercentage = aUBLTaxAmount.divide (aUBLTaxableAmount).multiply (CGlobal.BIGDEC_100);
          }

          // Save item and put in map
          final String sUBLTaxSchemeSchemeID = aUBLTaxCategory.getTaxScheme ().getID ().getSchemeID ();
          final String sUBLTaxSchemeID = aUBLTaxCategory.getTaxScheme ().getID ().getValue ();

          final String sUBLTaxCategorySchemeID = aUBLTaxCategory.getID ().getSchemeID ();
          final String sUBLTaxCategoryID = aUBLTaxCategory.getID ().getValue ();

          aTaxCategoryPercMap.put (new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                       new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID)),
                                   aUBLPercentage);

          if (_isSupportedTaxSchemeSchemeID (sUBLTaxSchemeSchemeID) && SUPPORTED_TAX_SCHEME_ID.equals (sUBLTaxSchemeID)) {
            // add VAT item
            final ItemType aNewVATItem = aObjectFactory.createItemType ();
            // Base amount
            aNewVATItem.setTaxedAmount (aUBLSubtotal.getTaxableAmountValue ());
            // tax rate
            final TaxRateType aNewVATTaxRate = aObjectFactory.createTaxRateType ();
            aNewVATTaxRate.setTaxCode (sUBLTaxCategoryID);
            aNewVATTaxRate.setValue (aUBLPercentage);
            aNewVATItem.setTaxRate (aNewVATTaxRate);
            // Tax amount
            aNewVATItem.setAmount (aUBLSubtotal.getTaxAmountValue ());
            // Add to list
            aNewVAT.getItem ().add (aNewVATItem);
          }
          else {
            // TODO other tax scheme
            s_aLogger.error ("Other tax scheme found and ignored: '" +
                             sUBLTaxSchemeSchemeID +
                             "' and '" +
                             sUBLTaxSchemeID +
                             "'");
          }
        }

      aNewTax.setVAT (aNewVAT);
      aNewInvoice.setTax (aNewTax);
    }

    // Line items
    BigDecimal aTotalZeroPercLineExtensionAmount = BigDecimal.ZERO;
    {
      final DetailsType aNewDetails = aObjectFactory.createDetailsType ();
      final ItemListType aNewItemList = aObjectFactory.createItemListType ();
      int nInvoiceLineIndex = 1;
      for (final InvoiceLineType aUBLInvoiceLine : aUBLInvoice.getInvoiceLine ()) {
        // Try to resolve tax category
        TaxCategoryType aUBLTaxCategory = ContainerHelper.getSafe (aUBLInvoiceLine.getItem ()
                                                                                  .getClassifiedTaxCategory (), 0);
        if (aUBLTaxCategory == null) {
          // No direct tax category -> check if it is somewhere in the tax total
          outer: for (final TaxTotalType aUBLTaxTotal : aUBLInvoiceLine.getTaxTotal ())
            for (final TaxSubtotalType aUBLTaxSubTotal : aUBLTaxTotal.getTaxSubtotal ()) {
              aUBLTaxCategory = aUBLTaxSubTotal.getTaxCategory ();
              if (aUBLTaxCategory != null) {
                // We found one -> just use it
                break outer;
              }
            }
        }

        // Try to resolve tax percentage
        BigDecimal aUBLPercent = null;
        if (aUBLTaxCategory != null) {
          // Specified at tax category?
          if (aUBLTaxCategory.getPercent () != null)
            aUBLPercent = aUBLTaxCategory.getPercentValue ();

          if (aUBLPercent == null &&
              aUBLTaxCategory.getID () != null &&
              aUBLTaxCategory.getTaxScheme () != null &&
              aUBLTaxCategory.getTaxScheme ().getID () != null) {
            // Not specified - check from previous map
            final String sUBLTaxSchemeSchemeID = aUBLTaxCategory.getTaxScheme ().getID ().getSchemeID ();
            final String sUBLTaxSchemeID = aUBLTaxCategory.getTaxScheme ().getIDValue ();

            final String sUBLTaxCategorySchemeID = aUBLTaxCategory.getID ().getSchemeID ();
            final String sUBLTaxCategoryID = aUBLTaxCategory.getIDValue ();

            final TaxCategoryKey aKey = new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                            new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID));
            aUBLPercent = aTaxCategoryPercMap.get (aKey);
          }
        }
        if (aUBLPercent == null) {
          s_aLogger.warn ("Failed to resolve tax percentage for invoice line! Using default 0.");
          aUBLPercent = BigDecimal.ZERO;
        }

        // Start creating ebInterface line
        final ListLineItemType aNewListLineItem = aObjectFactory.createListLineItemType ();

        // Invoice line number
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (aUBLInvoiceLine.getIDValue ());
        if (aUBLPositionNumber == null) {
          s_aLogger.warn ("Failed to parse UBL invoice line '" +
                          aUBLInvoiceLine.getIDValue () +
                          "' into a numeric value. Defaulting to index " +
                          nInvoiceLineIndex);
          aUBLPositionNumber = BigInteger.valueOf (nInvoiceLineIndex);
        }
        aNewListLineItem.setPositionNumber (aUBLPositionNumber);

        // Descriptions
        for (final DescriptionType aUBLDescription : aUBLInvoiceLine.getItem ().getDescription ())
          aNewListLineItem.getDescription ().add (aUBLDescription.getValue ());
        if (aNewListLineItem.getDescription ().isEmpty ()) {
          // Use item name as description
          final NameType aUBLName = aUBLInvoiceLine.getItem ().getName ();
          if (aUBLName != null)
            aNewListLineItem.getDescription ().add (aUBLName.getValue ());
        }

        // Quantity
        final UnitType aNewQuantity = aObjectFactory.createUnitType ();
        if (aUBLInvoiceLine.getInvoicedQuantity () != null) {
          aNewQuantity.setUnit (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode ().value ());
          aNewQuantity.setValue (aUBLInvoiceLine.getInvoicedQuantityValue ());
        }
        else {
          // ebInterface requires a quantity!
          // XXX is this correct as the default?
          aNewQuantity.setUnit (EUnitOfMeasureCode20.C62.getID ());
          aNewQuantity.setValue (BigDecimal.ONE);
        }
        aNewListLineItem.setQuantity (aNewQuantity);
        // Unit price
        if (aUBLInvoiceLine.getPrice () != null) {
          // Unit price = priceAmount/baseQuantity
          final BigDecimal aUBLPriceAmount = aUBLInvoiceLine.getPrice ().getPriceAmountValue ();
          // If no base quantity is present, assume 1
          final BigDecimal aUBLBaseQuantity = aUBLInvoiceLine.getPrice ().getBaseQuantityValue ();
          aNewListLineItem.setUnitPrice (aUBLBaseQuantity == null ? aUBLPriceAmount
                                                                 : aUBLPriceAmount.divide (aUBLBaseQuantity));
        }
        else {
          // Unit price = lineExtensionAmount / quantity
          final BigDecimal aUBLLineExtensionAmount = aUBLInvoiceLine.getLineExtensionAmountValue ();
          aNewListLineItem.setUnitPrice (aUBLLineExtensionAmount.divide (aNewQuantity.getValue ()));
        }

        // Tax rate (mandatory)
        final TaxRateType aNewTaxRate = aObjectFactory.createTaxRateType ();
        aNewTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          aNewTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aNewListLineItem.setTaxRate (aNewTaxRate);

        // Line item amount (quantity * unit price)
        aNewListLineItem.setLineItemAmount (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (aUBLPercent.equals (BigDecimal.ZERO))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Order reference for detail
        final OrderLineReferenceType aOrderLineReference = ContainerHelper.getFirstElement (aUBLInvoiceLine.getOrderLineReference ());
        if (aOrderLineReference != null) {
          final OrderReferenceDetailType aNewOrderReferenceDetail = aObjectFactory.createOrderReferenceDetailType ();
          aNewOrderReferenceDetail.setOrderID (sOrderReferenceID);
          aNewOrderReferenceDetail.setOrderPositionNumber (aOrderLineReference.getLineIDValue ());
          aNewListLineItem.setInvoiceRecipientsOrderReference (aNewOrderReferenceDetail);
        }

        // Add the item to the list
        aNewItemList.getListLineItem ().add (aNewListLineItem);
        nInvoiceLineIndex++;
      }
      aNewDetails.getItemList ().add (aNewItemList);
      aNewInvoice.setDetails (aNewDetails);
    }

    if (aNewVAT.hasNoItemEntries ()) {
      s_aLogger.warn ("No VAT item found. Defaulting to a single entry with 0% for amount " +
                      aTotalZeroPercLineExtensionAmount.toString ());
      final ItemType aNewVATItem = aObjectFactory.createItemType ();
      aNewVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
      final TaxRateType aNewVATTaxRate = aObjectFactory.createTaxRateType ();
      aNewVATTaxRate.setValue (BigDecimal.ZERO);
      aNewVATItem.setTaxRate (aNewVATTaxRate);
      aNewVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
      aNewVAT.getItem ().add (aNewVATItem);
    }

    // Total gross amount
    aNewInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Payment method
    {
      for (final PaymentMeansType aUBLPaymentMeans : aUBLInvoice.getPaymentMeans ()) {
        // Is a payment channel code present?
        if (PAYMENT_CHANNEL_CODE_IBAN.equals (aUBLPaymentMeans.getPaymentChannelCodeValue ())) {
          final UniversalBankTransactionType aNewUBTMethod = aObjectFactory.createUniversalBankTransactionType ();
          // Beneficiary account
          final AccountType aNewAccount = aObjectFactory.createAccountType ();

          // BIC
          aNewAccount.setBIC (aUBLPaymentMeans.getPayeeFinancialAccount ()
                                              .getFinancialInstitutionBranch ()
                                              .getFinancialInstitution ()
                                              .getIDValue ());
          if (!RegExHelper.stringMatchesPattern (REGEX_BIC, aNewAccount.getBIC ())) {
            s_aLogger.error ("The BIC '" + aNewAccount.getBIC () + "' does not match the required regular expression.");
            aNewAccount.setBIC (null);
          }

          // IBAN
          aNewAccount.setIBAN (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue ());
          if (StringHelper.getLength (aNewAccount.getIBAN ()) > IBAN_MAX_LENGTH) {
            s_aLogger.warn ("The IBAN '" +
                            aNewAccount.getIBAN () +
                            "' is too long and cut to " +
                            IBAN_MAX_LENGTH +
                            " chars.");
            aNewAccount.setIBAN (aNewAccount.getIBAN ().substring (0, IBAN_MAX_LENGTH));
          }

          aNewUBTMethod.getBeneficiaryAccount ().add (aNewAccount);
          aNewInvoice.setPaymentMethod (aNewUBTMethod);
          break;
        }
      }
    }

    return aNewInvoice;
  }
}
