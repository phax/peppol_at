package at.gv.brz.transform.ubl2ebi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.FinancialAccountType;
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
import com.phloc.commons.math.MathHelper;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.ebinterface.v40.AccountType;
import com.phloc.ebinterface.v40.AddressType;
import com.phloc.ebinterface.v40.BillerType;
import com.phloc.ebinterface.v40.CountryCodeType;
import com.phloc.ebinterface.v40.CountryType;
import com.phloc.ebinterface.v40.CurrencyType;
import com.phloc.ebinterface.v40.DetailsType;
import com.phloc.ebinterface.v40.DocumentTypeType;
import com.phloc.ebinterface.v40.InvoiceRecipientType;
import com.phloc.ebinterface.v40.InvoiceType;
import com.phloc.ebinterface.v40.ItemListType;
import com.phloc.ebinterface.v40.ItemType;
import com.phloc.ebinterface.v40.ListLineItemType;
import com.phloc.ebinterface.v40.ObjectFactory;
import com.phloc.ebinterface.v40.OrderReferenceDetailType;
import com.phloc.ebinterface.v40.OrderReferenceType;
import com.phloc.ebinterface.v40.TaxRateType;
import com.phloc.ebinterface.v40.TaxType;
import com.phloc.ebinterface.v40.UnitType;
import com.phloc.ebinterface.v40.UniversalBankTransactionType;
import com.phloc.ebinterface.v40.VATType;
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
public final class PEPPOLUBL20ToEbInterface40Converter {
  private static final Logger s_aLogger = LoggerFactory.getLogger (PEPPOLUBL20ToEbInterface40Converter.class);
  private static final String DUMMY_VALUE = "DUMMY_VALUE";
  private static final String REGEX_BIC = "[0-9A-Za-z]{8}([0-9A-Za-z]{3})?";
  private static final String SUPPORTED_TAX_SCHEME_SCHEME_ID = "UN/ECE 5153";
  private static final int IBAN_MAX_LENGTH = 34;
  private static final String PAYMENT_CHANNEL_CODE_IBAN = "IBAN";
  private static final String SUPPORTED_TAX_SCHEME_ID = "VAT";
  private static final String EBI_GENERATING_SYSTEM = "UBL 2.0 to ebInterface 4.0 converter";
  private static final int SCALE_PERC = 2;
  private static final int SCALE_PRICE_LINE = 4;
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

  private PEPPOLUBL20ToEbInterface40Converter () {}

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
      return "Invalid UBLVersionID value '" + aUBLVersionID.getValue () + "' present!";

    // Check ProfileID
    final ProfileIDType aProfileID = aUBLInvoice.getProfileID ();
    if (aProfileID == null)
      return "No ProfileID present!";
    final String sProfileID = StringHelper.trim (aProfileID.getValue ());
    final IPeppolPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
    if (aProcID == null)
      return "Invalid ProfileID value '" + sProfileID + "' present!";

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false) {
      final CustomizationIDType aCustomizationID = aUBLInvoice.getCustomizationID ();
      if (aCustomizationID == null)
        return "No CustomizationID present!";
      if (!CPeppolUBL.CUSTOMIZATION_SCHEMEID.equals (aCustomizationID.getSchemeID ()))
        return "Invalid CustomizationID schemeID '" + aCustomizationID.getSchemeID () + "' present!";
      final String sCustomizationID = aCustomizationID.getValue ();
      IPeppolPredefinedDocumentTypeIdentifier aMatchingDocID = null;
      for (final IPeppolPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
        if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID)) {
          // We found a match
          aMatchingDocID = aDocID;
          break;
        }
      if (aMatchingDocID == null)
        return "Invalid CustomizationID value '" + sCustomizationID + "' present!";
    }

    // Invoice type code
    final InvoiceTypeCodeType aInvoiceTypeCode = aUBLInvoice.getInvoiceTypeCode ();
    if (aInvoiceTypeCode == null) {
      // None present
      s_aLogger.warn ("No InvoiceTypeCode present! Assuming " + CPeppolUBL.INVOICE_TYPE_CODE);
    }
    else {
      // If one is present, it must match
      final String sInvoiceTypeCode = aInvoiceTypeCode.getValue ().trim ();
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (sInvoiceTypeCode))
        s_aLogger.error ("Invalid InvoiceTypeCode value '" +
                         sInvoiceTypeCode.trim () +
                         "' present! Expected '" +
                         CPeppolUBL.INVOICE_TYPE_CODE +
                         "'");
    }

    // Done
    return null;
  }

  @Nonnull
  private static AddressType _convertAddress (final ObjectFactory aObjectFactory, final PartyType aUBLParty) {
    final AddressType aEbiAddress = aObjectFactory.createAddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      aEbiAddress.setName (aUBLPartyName.getNameValue ());
      if (aUBLParty.getPartyNameCount () > 1)
        s_aLogger.warn ("UBL invoice has multiple party names - only the first one is used!");
    }

    // Convert main address
    final oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType aUBLAddress = aUBLParty.getPostalAddress ();
    if (aUBLAddress != null) {
      aEbiAddress.setStreet (StringHelper.getImplodedNonEmpty (" ",
                                                               aUBLAddress.getStreetNameValue (),
                                                               aUBLAddress.getBuildingNumberValue ()));
      aEbiAddress.setPOBox (aUBLAddress.getPostboxValue ());
      aEbiAddress.setTown (aUBLAddress.getCityNameValue ());
      aEbiAddress.setZIP (aUBLAddress.getPostalZoneValue ());
      if (aUBLAddress.getCountry () != null) {
        final CountryType aEbiCountry = new CountryType ();
        CountryCodeType aCC = null;
        try {
          aCC = CountryCodeType.fromValue (aUBLAddress.getCountry ().getIdentificationCodeValue ());
        }
        catch (final IllegalArgumentException ex) {}
        aEbiCountry.setCountryCode (aCC);
        aEbiCountry.setContent (aUBLAddress.getCountry ().getNameValue ());
        aEbiAddress.setCountry (aEbiCountry);
      }
    }

    // Contact
    final ContactType aUBLContact = aUBLParty.getContact ();
    if (aUBLContact != null) {
      aEbiAddress.setPhone (aUBLContact.getTelephoneValue ());
      aEbiAddress.setEmail (aUBLContact.getElectronicMailValue ());
    }

    // Person name
    final PersonType aUBLPerson = aUBLParty.getPerson ();
    if (aUBLPerson != null) {
      aEbiAddress.setContact (StringHelper.getImplodedNonEmpty (" ",
                                                                aUBLPerson.getTitleValue (),
                                                                aUBLPerson.getFirstNameValue (),
                                                                aUBLPerson.getMiddleNameValue (),
                                                                aUBLPerson.getFamilyNameValue (),
                                                                aUBLPerson.getNameSuffixValue ()));
    }

    // Check all mandatory fields
    if (aEbiAddress.getName () == null)
      aEbiAddress.setName (DUMMY_VALUE);
    if (aEbiAddress.getStreet () == null)
      aEbiAddress.setStreet (DUMMY_VALUE);
    if (aEbiAddress.getTown () == null)
      aEbiAddress.setTown (DUMMY_VALUE);
    if (aEbiAddress.getZIP () == null)
      aEbiAddress.setZIP (DUMMY_VALUE);
    if (aEbiAddress.getCountry () == null) {
      final CountryType aCountry = new CountryType ();
      aCountry.setContent (DUMMY_VALUE);
      aEbiAddress.setCountry (aCountry);
    }

    return aEbiAddress;
  }

  private static boolean _isSupportedTaxSchemeSchemeID (@Nullable final String sUBLTaxSchemeSchemeID) {
    return sUBLTaxSchemeSchemeID == null ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID) ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID + " Subset");
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
    final InvoiceType aEbiInvoice = aObjectFactory.createInvoiceType ();
    aEbiInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiInvoice.setDocumentType (DocumentTypeType.INVOICE);
    aEbiInvoice.setInvoiceCurrency (CurrencyType.fromValue (StringHelper.trim (aUBLInvoice.getDocumentCurrencyCodeValue ())));
    aEbiInvoice.setInvoiceNumber (aUBLInvoice.getIDValue ());
    aEbiInvoice.setInvoiceDate (aUBLInvoice.getIssueDateValue ());

    // Biller (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLInvoice.getAccountingSupplierParty ();
      final BillerType aEbiBiller = aObjectFactory.createBillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ().equals (SUPPORTED_TAX_SCHEME_ID)) {
          aEbiBiller.setVATIdentificationNumber (aUBLPartyTaxScheme.getCompanyIDValue ());
          break;
        }
      if (aEbiBiller.getVATIdentificationNumber () == null) {
        // A VAT number must be present!
        s_aLogger.error ("Failed to get biller VAT number!");
        aEbiBiller.setVATIdentificationNumber (DUMMY_VALUE);
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null) {
        // The customer's internal identifier for the supplier.
        aEbiBiller.setInvoiceRecipientsBillerID (aUBLSupplier.getCustomerAssignedAccountID ().getValue ());
      }
      aEbiBiller.setAddress (_convertAddress (aObjectFactory, aUBLSupplier.getParty ()));
      aEbiInvoice.setBiller (aEbiBiller);
    }

    // Invoice recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLInvoice.getAccountingCustomerParty ();
      final InvoiceRecipientType aEbiRecipient = aObjectFactory.createInvoiceRecipientType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLCustomer.getParty ().getPartyTaxScheme ())
        if (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ().equals (SUPPORTED_TAX_SCHEME_ID)) {
          aEbiRecipient.setVATIdentificationNumber (aUBLPartyTaxScheme.getCompanyIDValue ());
          break;
        }
      if (aEbiRecipient.getVATIdentificationNumber () == null) {
        // Mandatory field
        s_aLogger.error ("Failed to get supplier VAT number!");
        aEbiRecipient.setVATIdentificationNumber (DUMMY_VALUE);
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null) {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        aEbiRecipient.setBillersInvoiceRecipientID (aUBLCustomer.getSupplierAssignedAccountIDValue ());
      }
      else {
        // Mandatory field
        s_aLogger.error ("Failed to get supplier assigned account ID for customer!");
        aEbiRecipient.setBillersInvoiceRecipientID (DUMMY_VALUE);
      }
      aEbiRecipient.setAddress (_convertAddress (aObjectFactory, aUBLCustomer.getParty ()));
      aEbiInvoice.setInvoiceRecipient (aEbiRecipient);
    }

    // Order reference of invoice recipient
    String sUBLOrderReferenceID = null;
    {
      final oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderReferenceType aUBLOrderReference = aUBLInvoice.getOrderReference ();
      if (aUBLOrderReference != null) {
        // Use directly from order reference
        sUBLOrderReferenceID = aUBLOrderReference.getIDValue ();
      }
      else {
        // Check if a contract reference is present
        final List <DocumentReferenceType> aUBLContractDocumentReferences = aUBLInvoice.getContractDocumentReference ();
        if (!aUBLContractDocumentReferences.isEmpty ()) {
          // ID is mandatory
          sUBLOrderReferenceID = ContainerHelper.getFirstElement (aUBLContractDocumentReferences).getIDValue ();
        }
      }

      if (StringHelper.hasNoText (sUBLOrderReferenceID)) {
        s_aLogger.error ("Failed to get order reference ID!");
        sUBLOrderReferenceID = DUMMY_VALUE;
      }
      else {
        if (sUBLOrderReferenceID.length () > 35) {
          s_aLogger.error ("Order reference value '" +
                           sUBLOrderReferenceID +
                           "' is too long. It will be cut to 35 characters.");
          sUBLOrderReferenceID = sUBLOrderReferenceID.substring (0, 35);
        }

        sUBLOrderReferenceID = _makeAlphaNumIDType (sUBLOrderReferenceID);
      }

      final OrderReferenceType aEbiOrderReference = aObjectFactory.createOrderReferenceType ();
      aEbiOrderReference.setOrderID (sUBLOrderReferenceID);
      aEbiInvoice.getInvoiceRecipient ().setOrderReference (aEbiOrderReference);
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final TaxType aEbiTax = aObjectFactory.createTaxType ();
    final VATType aEbiVAT = aObjectFactory.createVATType ();
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
              aUBLPercentage = aUBLTaxAmount.multiply (CGlobal.BIGDEC_100).divide (aUBLTaxableAmount,
                                                                                   SCALE_PERC,
                                                                                   ROUNDING_MODE);
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
            final ItemType aEbiVATItem = aObjectFactory.createItemType ();
            // Base amount
            aEbiVATItem.setTaxedAmount (aUBLSubtotal.getTaxableAmountValue ());
            // tax rate
            final TaxRateType aEbiVATTaxRate = aObjectFactory.createTaxRateType ();
            // Optional
            if (false)
              aEbiVATTaxRate.setTaxCode (sUBLTaxCategoryID);
            aEbiVATTaxRate.setValue (aUBLPercentage);
            aEbiVATItem.setTaxRate (aEbiVATTaxRate);
            // Tax amount
            aEbiVATItem.setAmount (aUBLSubtotal.getTaxAmountValue ());
            // Add to list
            aEbiVAT.getItem ().add (aEbiVATItem);
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

      aEbiTax.setVAT (aEbiVAT);
      aEbiInvoice.setTax (aEbiTax);
    }

    // Line items
    BigDecimal aTotalZeroPercLineExtensionAmount = BigDecimal.ZERO;
    {
      final DetailsType aEbiDetails = aObjectFactory.createDetailsType ();
      final ItemListType aEbiItemList = aObjectFactory.createItemListType ();
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
        final ListLineItemType aEbiListLineItem = aObjectFactory.createListLineItemType ();

        // Invoice line number
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (aUBLInvoiceLine.getIDValue ());
        if (aUBLPositionNumber == null) {
          s_aLogger.warn ("Failed to parse UBL invoice line '" +
                          aUBLInvoiceLine.getIDValue () +
                          "' into a numeric value. Defaulting to index " +
                          nInvoiceLineIndex);
          aUBLPositionNumber = BigInteger.valueOf (nInvoiceLineIndex);
        }
        aEbiListLineItem.setPositionNumber (aUBLPositionNumber);

        // Descriptions
        for (final DescriptionType aUBLDescription : aUBLInvoiceLine.getItem ().getDescription ())
          aEbiListLineItem.getDescription ().add (aUBLDescription.getValue ());
        if (aEbiListLineItem.getDescription ().isEmpty ()) {
          // Use item name as description
          final NameType aUBLName = aUBLInvoiceLine.getItem ().getName ();
          if (aUBLName != null)
            aEbiListLineItem.getDescription ().add (aUBLName.getValue ());
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
        aEbiListLineItem.setQuantity (aNewQuantity);

        // Unit price
        if (aUBLInvoiceLine.getPrice () != null) {
          // Unit price = priceAmount/baseQuantity
          final BigDecimal aUBLPriceAmount = aUBLInvoiceLine.getPrice ().getPriceAmountValue ();
          // If no base quantity is present, assume 1
          final BigDecimal aUBLBaseQuantity = aUBLInvoiceLine.getPrice ().getBaseQuantityValue ();
          aEbiListLineItem.setUnitPrice (aUBLBaseQuantity == null ? aUBLPriceAmount
                                                                 : MathHelper.isEqualToZero (aUBLBaseQuantity) ? BigDecimal.ZERO
                                                                                                              : aUBLPriceAmount.divide (aUBLBaseQuantity,
                                                                                                                                        SCALE_PRICE_LINE,
                                                                                                                                        ROUNDING_MODE));
        }
        else {
          // Unit price = lineExtensionAmount / quantity
          final BigDecimal aUBLLineExtensionAmount = aUBLInvoiceLine.getLineExtensionAmountValue ();
          aEbiListLineItem.setUnitPrice (aUBLLineExtensionAmount.divide (aNewQuantity.getValue ()));
        }

        // Tax rate (mandatory)
        final TaxRateType aNewTaxRate = aObjectFactory.createTaxRateType ();
        aNewTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          // Optional
          if (false)
            aNewTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aEbiListLineItem.setTaxRate (aNewTaxRate);

        // Line item amount (quantity * unit price)
        aEbiListLineItem.setLineItemAmount (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (aUBLPercent.equals (BigDecimal.ZERO))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Order reference for detail
        final OrderLineReferenceType aOrderLineReference = ContainerHelper.getFirstElement (aUBLInvoiceLine.getOrderLineReference ());
        if (aOrderLineReference != null) {
          final OrderReferenceDetailType aNewOrderReferenceDetail = aObjectFactory.createOrderReferenceDetailType ();
          aNewOrderReferenceDetail.setOrderID (sUBLOrderReferenceID);
          aNewOrderReferenceDetail.setOrderPositionNumber (aOrderLineReference.getLineIDValue ());
          aEbiListLineItem.setInvoiceRecipientsOrderReference (aNewOrderReferenceDetail);
        }

        // Add the item to the list
        aEbiItemList.getListLineItem ().add (aEbiListLineItem);
        nInvoiceLineIndex++;
      }
      aEbiDetails.getItemList ().add (aEbiItemList);
      aEbiInvoice.setDetails (aEbiDetails);
    }

    if (aEbiVAT.hasNoItemEntries ()) {
      s_aLogger.warn ("No VAT item found. Defaulting to a single entry with 0% for amount " +
                      aTotalZeroPercLineExtensionAmount.toString ());
      final ItemType aNewVATItem = aObjectFactory.createItemType ();
      aNewVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
      final TaxRateType aNewVATTaxRate = aObjectFactory.createTaxRateType ();
      aNewVATTaxRate.setValue (BigDecimal.ZERO);
      aNewVATItem.setTaxRate (aNewVATTaxRate);
      aNewVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
      aEbiVAT.getItem ().add (aNewVATItem);
    }

    // Total gross amount
    aEbiInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Payment method
    {
      for (final PaymentMeansType aUBLPaymentMeans : aUBLInvoice.getPaymentMeans ()) {
        // Is a payment channel code present?
        if (PAYMENT_CHANNEL_CODE_IBAN.equals (aUBLPaymentMeans.getPaymentChannelCodeValue ())) {
          final UniversalBankTransactionType aNewUBTMethod = aObjectFactory.createUniversalBankTransactionType ();
          // Beneficiary account
          final AccountType aNewAccount = aObjectFactory.createAccountType ();

          // BIC
          final FinancialAccountType aUBLFinancialAccount = aUBLPaymentMeans.getPayeeFinancialAccount ();
          if (aUBLFinancialAccount.getFinancialInstitutionBranch () != null &&
              aUBLFinancialAccount.getFinancialInstitutionBranch ().getFinancialInstitution () != null) {
            aNewAccount.setBIC (aUBLFinancialAccount.getFinancialInstitutionBranch ()
                                                    .getFinancialInstitution ()
                                                    .getIDValue ());
            if (!RegExHelper.stringMatchesPattern (REGEX_BIC, aNewAccount.getBIC ())) {
              s_aLogger.error ("The BIC '" +
                               aNewAccount.getBIC () +
                               "' does not match the required regular expression.");
              aNewAccount.setBIC (null);
            }
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
          aEbiInvoice.setPaymentMethod (aNewUBTMethod);
          break;
        }
      }
    }

    return aEbiInvoice;
  }

  @Nullable
  private static String _makeAlphaNumIDType (@Nullable final String sText) {
    if (sText != null && !RegExHelper.stringMatchesPattern ("[0-9 | A-Z | a-z | -_äöüÄÖÜß]+", sText)) {
      s_aLogger.warn ("'" + sText + "' is not an AlphaNumIDType!");
      final String ret = RegExHelper.stringReplacePattern ("[^0-9 | A-Z | a-z | -_äöüÄÖÜß]", sText, "_");
      s_aLogger.warn ("  -> was changed to '" + ret + "'");
      return ret;
    }
    return sText;
  }
}
