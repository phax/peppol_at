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

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.FinancialAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyIdentificationType;
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
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.gv.brz.transform.ubl2ebi.helper.SchemedID;
import at.gv.brz.transform.ubl2ebi.helper.TaxCategoryKey;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.math.MathHelper;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.state.ETriState;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.ebinterface.codelist.ETaxCode;
import com.phloc.ebinterface.v40.Ebi40AccountType;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierType;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierTypeType;
import com.phloc.ebinterface.v40.Ebi40AddressType;
import com.phloc.ebinterface.v40.Ebi40BillerType;
import com.phloc.ebinterface.v40.Ebi40CountryCodeType;
import com.phloc.ebinterface.v40.Ebi40CountryType;
import com.phloc.ebinterface.v40.Ebi40CurrencyType;
import com.phloc.ebinterface.v40.Ebi40DetailsType;
import com.phloc.ebinterface.v40.Ebi40DocumentTypeType;
import com.phloc.ebinterface.v40.Ebi40InvoiceRecipientType;
import com.phloc.ebinterface.v40.Ebi40InvoiceType;
import com.phloc.ebinterface.v40.Ebi40ItemListType;
import com.phloc.ebinterface.v40.Ebi40ItemType;
import com.phloc.ebinterface.v40.Ebi40ListLineItemType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceDetailType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeBaseType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeDetailsType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeListLineItemDetailsType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeType;
import com.phloc.ebinterface.v40.Ebi40TaxRateType;
import com.phloc.ebinterface.v40.Ebi40TaxType;
import com.phloc.ebinterface.v40.Ebi40UnitType;
import com.phloc.ebinterface.v40.Ebi40UniversalBankTransactionType;
import com.phloc.ebinterface.v40.Ebi40VATType;
import com.phloc.ebinterface.v40.ObjectFactory;
import com.phloc.ubl20.codelist.EUnitOfMeasureCode20;

import eu.europa.ec.cipa.peppol.identifier.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.IPeppolPredefinedProcessIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.PredefinedProcessIdentifierManager;

/**
 * Main converter between UBL 2.0 invoice and ebInterface 4.0 invoice.
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
  private static String _checkConsistency (final InvoiceType aUBLInvoice) {
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
  private static Ebi40AddressType _convertAddress (@Nonnull final PartyType aUBLParty) {
    final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      aEbiAddress.setName (aUBLPartyName.getNameValue ());
      if (aUBLParty.getPartyNameCount () > 1)
        s_aLogger.warn ("UBL invoice has multiple party names - only the first one is used!");
    }

    // Convert main address
    final AddressType aUBLAddress = aUBLParty.getPostalAddress ();
    if (aUBLAddress != null) {
      aEbiAddress.setStreet (StringHelper.getImplodedNonEmpty (" ",
                                                               aUBLAddress.getStreetNameValue (),
                                                               aUBLAddress.getBuildingNumberValue ()));
      aEbiAddress.setPOBox (aUBLAddress.getPostboxValue ());
      aEbiAddress.setTown (aUBLAddress.getCityNameValue ());
      aEbiAddress.setZIP (aUBLAddress.getPostalZoneValue ());
      if (aUBLAddress.getCountry () != null) {
        final Ebi40CountryType aEbiCountry = new Ebi40CountryType ();
        Ebi40CountryCodeType aCC = null;
        try {
          aCC = Ebi40CountryCodeType.fromValue (aUBLAddress.getCountry ().getIdentificationCodeValue ());
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

    // GLN and DUNS number
    if (aUBLParty.getEndpointID () != null) {
      final String sEndpointID = aUBLParty.getEndpointIDValue ();
      if (StringHelper.hasText (sEndpointID)) {
        // We have an endpoint ID
        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (aUBLParty.getEndpointID ().getSchemeID ())) {
            final Ebi40AddressIdentifierType aEbiType = new Ebi40AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setContent (sEndpointID);
            aEbiAddress.setAddressIdentifier (aEbiType);
            break;
          }

        if (aEbiAddress.getAddressIdentifier () == null)
          s_aLogger.warn ("Ignoring party endpoint ID '" +
                          sEndpointID +
                          "' of type '" +
                          aUBLParty.getEndpointID ().getSchemeID () +
                          "'");
      }
    }

    if (aEbiAddress.getAddressIdentifier () == null) {
      // check party identification
      outer: for (final PartyIdentificationType aUBLPartyID : aUBLParty.getPartyIdentification ()) {
        final String sUBLPartyID = aUBLPartyID.getIDValue ();
        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (aUBLPartyID.getID ().getSchemeID ())) {
            // Add GLN/DUNS number
            final Ebi40AddressIdentifierType aEbiType = new Ebi40AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setContent (sUBLPartyID);
            aEbiAddress.setAddressIdentifier (aEbiType);
            break outer;
          }
        s_aLogger.warn ("Ignoring party identification '" +
                        sUBLPartyID +
                        "' of type '" +
                        aUBLPartyID.getID ().getSchemeID () +
                        "'");
      }
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
      final Ebi40CountryType aCountry = new Ebi40CountryType ();
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
  public static Ebi40InvoiceType convertToEbInterface (@Nonnull final InvoiceType aUBLInvoice) {
    if (aUBLInvoice == null)
      throw new NullPointerException ("UBLInvoice");

    // Consistency check before starting the conversion
    final String sConsistencyValidationResult = _checkConsistency (aUBLInvoice);
    if (sConsistencyValidationResult != null)
      throw new IllegalArgumentException ("Consistency validation failed: " + sConsistencyValidationResult);

    // Build ebInterface invoice
    final Ebi40InvoiceType aEbiInvoice = new Ebi40InvoiceType ();
    aEbiInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiInvoice.setDocumentType (Ebi40DocumentTypeType.INVOICE);
    aEbiInvoice.setInvoiceCurrency (Ebi40CurrencyType.fromValue (StringHelper.trim (aUBLInvoice.getDocumentCurrencyCodeValue ())));
    aEbiInvoice.setInvoiceNumber (aUBLInvoice.getIDValue ());
    aEbiInvoice.setInvoiceDate (aUBLInvoice.getIssueDateValue ());

    // Biller (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLInvoice.getAccountingSupplierParty ();
      final Ebi40BillerType aEbiBiller = new Ebi40BillerType ();
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
      aEbiBiller.setAddress (_convertAddress (aUBLSupplier.getParty ()));
      aEbiInvoice.setBiller (aEbiBiller);
    }

    // Invoice recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLInvoice.getAccountingCustomerParty ();
      final Ebi40InvoiceRecipientType aEbiRecipient = new Ebi40InvoiceRecipientType ();
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
      aEbiRecipient.setAddress (_convertAddress (aUBLCustomer.getParty ()));
      aEbiInvoice.setInvoiceRecipient (aEbiRecipient);
    }

    // Order reference of invoice recipient
    String sUBLOrderReferenceID = null;
    {
      final OrderReferenceType aUBLOrderReference = aUBLInvoice.getOrderReference ();
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

      final Ebi40OrderReferenceType aEbiOrderReference = new Ebi40OrderReferenceType ();
      aEbiOrderReference.setOrderID (sUBLOrderReferenceID);
      aEbiInvoice.getInvoiceRecipient ().setOrderReference (aEbiOrderReference);
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final Ebi40TaxType aEbiTax = new Ebi40TaxType ();
    final Ebi40VATType aEbiVAT = new Ebi40VATType ();
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
            final Ebi40ItemType aEbiVATItem = new Ebi40ItemType ();
            // Base amount
            aEbiVATItem.setTaxedAmount (aUBLSubtotal.getTaxableAmountValue ());
            // tax rate
            final Ebi40TaxRateType aEbiVATTaxRate = new Ebi40TaxRateType ();
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
      final Ebi40DetailsType aEbiDetails = new Ebi40DetailsType ();
      final Ebi40ItemListType aEbiItemList = new Ebi40ItemListType ();
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
        final Ebi40ListLineItemType aEbiListLineItem = new Ebi40ListLineItemType ();

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
        final Ebi40UnitType aEbiQuantity = new Ebi40UnitType ();
        if (aUBLInvoiceLine.getInvoicedQuantity () != null) {
          aEbiQuantity.setUnit (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode ().value ());
          aEbiQuantity.setValue (aUBLInvoiceLine.getInvoicedQuantityValue ());
        }
        else {
          // ebInterface requires a quantity!
          // XXX is this correct as the default?
          aEbiQuantity.setUnit (EUnitOfMeasureCode20.C62.getID ());
          aEbiQuantity.setValue (BigDecimal.ONE);
        }
        aEbiListLineItem.setQuantity (aEbiQuantity);

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
          aEbiListLineItem.setUnitPrice (aUBLLineExtensionAmount.divide (aEbiQuantity.getValue ()));
        }

        // Tax rate (mandatory)
        final Ebi40TaxRateType aEbiTaxRate = new Ebi40TaxRateType ();
        aEbiTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          // Optional
          if (false)
            aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aEbiListLineItem.setTaxRate (aEbiTaxRate);

        // Line item amount (quantity * unit price)
        aEbiListLineItem.setLineItemAmount (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (aUBLPercent.equals (BigDecimal.ZERO))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Order reference for detail
        final OrderLineReferenceType aOrderLineReference = ContainerHelper.getFirstElement (aUBLInvoiceLine.getOrderLineReference ());
        if (aOrderLineReference != null) {
          final Ebi40OrderReferenceDetailType aEbiOrderReferenceDetail = new Ebi40OrderReferenceDetailType ();
          aEbiOrderReferenceDetail.setOrderID (sUBLOrderReferenceID);
          aEbiOrderReferenceDetail.setOrderPositionNumber (aOrderLineReference.getLineIDValue ());
          aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderReferenceDetail);
        }

        // Reduction and surcharge
        if (aUBLInvoiceLine.hasAllowanceChargeEntries ()) {
          // Start with quantity*unitPrice for base amount
          BigDecimal aEbiBaseAmount = aEbiListLineItem.getQuantity ()
                                                      .getValue ()
                                                      .multiply (aEbiListLineItem.getUnitPrice ());
          final Ebi40ReductionAndSurchargeListLineItemDetailsType aEbiRSDetails = new Ebi40ReductionAndSurchargeListLineItemDetailsType ();

          // ebInterface can handle only Reduction or only Surcharge
          ETriState eSurcharge = ETriState.UNDEFINED;

          for (final AllowanceChargeType aUBLAllowanceCharge : aUBLInvoiceLine.getAllowanceCharge ()) {
            final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();
            if (eSurcharge.isUndefined ())
              eSurcharge = ETriState.valueOf (bItemIsSurcharge);
            final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();
            if (bSwapSigns)
              s_aLogger.warn ("Reduction/Surcharge is mixed in this invoice!");

            final Ebi40ReductionAndSurchargeBaseType aEbiRSItem = new Ebi40ReductionAndSurchargeBaseType ();
            final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
            aEbiRSItem.setAmount (bSwapSigns ? aAmount.negate () : aAmount);
            if (aUBLAllowanceCharge.getBaseAmount () != null)
              aEbiBaseAmount = aUBLAllowanceCharge.getBaseAmountValue ();
            aEbiRSItem.setBaseAmount (aEbiBaseAmount);
            if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null) {
              final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ()
                                                          .multiply (CGlobal.BIGDEC_100);
              aEbiRSItem.setPercentage (bSwapSigns ? aPerc.negate () : aPerc);
            }

            if (eSurcharge.isTrue ()) {
              // Surcharge
              aEbiRSDetails.getSurchargeListLineItem ().add (aEbiRSItem);

              // Update base amount with this item
              aEbiBaseAmount = aEbiBaseAmount.add (aUBLAllowanceCharge.getAmountValue ());
            }
            else {
              // Reduction
              aEbiRSDetails.getReductionListLineItem ().add (aEbiRSItem);

              // Update base amount with this item
              aEbiBaseAmount = aEbiBaseAmount.subtract (aUBLAllowanceCharge.getAmountValue ());
            }
          }
          aEbiListLineItem.setReductionAndSurchargeListLineItemDetails (aEbiRSDetails);
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
      final Ebi40ItemType aEbiVATItem = new Ebi40ItemType ();
      aEbiVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
      final Ebi40TaxRateType aEbiVATTaxRate = new Ebi40TaxRateType ();
      aEbiVATTaxRate.setValue (BigDecimal.ZERO);
      aEbiVATItem.setTaxRate (aEbiVATTaxRate);
      aEbiVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
      aEbiVAT.getItem ().add (aEbiVATItem);
    }

    // Global reduction and surcharge
    if (aUBLInvoice.hasAllowanceChargeEntries ()) {
      // Start with quantity*unitPrice for base amount
      BigDecimal aEbiBaseAmount = aUBLInvoice.getLegalMonetaryTotal ().getLineExtensionAmountValue ();
      final Ebi40ReductionAndSurchargeDetailsType aEbiRS = new Ebi40ReductionAndSurchargeDetailsType ();

      // ebInterface can handle only Reduction or only Surcharge
      ETriState eSurcharge = ETriState.UNDEFINED;

      for (final AllowanceChargeType aUBLAllowanceCharge : aUBLInvoice.getAllowanceCharge ()) {
        final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();
        if (eSurcharge.isUndefined ())
          eSurcharge = ETriState.valueOf (bItemIsSurcharge);
        final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();
        if (bSwapSigns)
          s_aLogger.warn ("Reduction/Surcharge is mixed in this invoice!");

        final Ebi40ReductionAndSurchargeType aEbiRSItem = new Ebi40ReductionAndSurchargeType ();
        final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
        aEbiRSItem.setAmount (bSwapSigns ? aAmount.negate () : aAmount);
        if (aUBLAllowanceCharge.getBaseAmount () != null)
          aEbiBaseAmount = aUBLAllowanceCharge.getBaseAmountValue ();
        aEbiRSItem.setBaseAmount (aEbiBaseAmount);
        if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null) {
          final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ().multiply (CGlobal.BIGDEC_100);
          aEbiRSItem.setPercentage (bSwapSigns ? aPerc.negate () : aPerc);
        }

        Ebi40TaxRateType aEbiTaxRate = null;
        for (final TaxCategoryType aUBLTaxCategory : aUBLAllowanceCharge.getTaxCategory ())
          if (aUBLTaxCategory.getPercent () != null) {
            aEbiTaxRate = new Ebi40TaxRateType ();
            aEbiTaxRate.setValue (aUBLTaxCategory.getPercentValue ());
            if (false)
              aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
            break;
          }
        if (aEbiTaxRate == null) {
          s_aLogger.warn ("Failed to resolve tax percentage for global AllowanceCharge! Using default of 0%.");
          aEbiTaxRate = new Ebi40TaxRateType ();
          aEbiTaxRate.setValue (BigDecimal.ZERO);
          aEbiTaxRate.setTaxCode (ETaxCode.NOT_TAXABLE.getID ());
        }
        aEbiRSItem.setTaxRate (aEbiTaxRate);

        if (eSurcharge.isTrue ()) {
          // Surcharge
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createSurcharge (aEbiRSItem));

          // Update base amount with this item
          aEbiBaseAmount = aEbiBaseAmount.add (aUBLAllowanceCharge.getAmountValue ());
        }
        else {
          // Reduction
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createReduction (aEbiRSItem));

          // Update base amount with this item
          aEbiBaseAmount = aEbiBaseAmount.subtract (aUBLAllowanceCharge.getAmountValue ());
        }
      }
      aEbiInvoice.setReductionAndSurchargeDetails (aEbiRS);
    }

    // Total gross amount
    aEbiInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Payment method
    {
      for (final PaymentMeansType aUBLPaymentMeans : aUBLInvoice.getPaymentMeans ()) {
        // Is a payment channel code present?
        if (PAYMENT_CHANNEL_CODE_IBAN.equals (aUBLPaymentMeans.getPaymentChannelCodeValue ())) {
          final Ebi40UniversalBankTransactionType aEbiUBTMethod = new Ebi40UniversalBankTransactionType ();
          // Beneficiary account
          final Ebi40AccountType aEbiAccount = new Ebi40AccountType ();

          // BIC
          final FinancialAccountType aUBLFinancialAccount = aUBLPaymentMeans.getPayeeFinancialAccount ();
          if (aUBLFinancialAccount.getFinancialInstitutionBranch () != null &&
              aUBLFinancialAccount.getFinancialInstitutionBranch ().getFinancialInstitution () != null) {
            aEbiAccount.setBIC (aUBLFinancialAccount.getFinancialInstitutionBranch ()
                                                    .getFinancialInstitution ()
                                                    .getIDValue ());
            if (!RegExHelper.stringMatchesPattern (REGEX_BIC, aEbiAccount.getBIC ())) {
              s_aLogger.error ("The BIC '" +
                               aEbiAccount.getBIC () +
                               "' does not match the required regular expression.");
              aEbiAccount.setBIC (null);
            }
          }

          // IBAN
          aEbiAccount.setIBAN (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue ());
          if (StringHelper.getLength (aEbiAccount.getIBAN ()) > IBAN_MAX_LENGTH) {
            s_aLogger.warn ("The IBAN '" +
                            aEbiAccount.getIBAN () +
                            "' is too long and cut to " +
                            IBAN_MAX_LENGTH +
                            " chars.");
            aEbiAccount.setIBAN (aEbiAccount.getIBAN ().substring (0, IBAN_MAX_LENGTH));
          }

          aEbiUBTMethod.getBeneficiaryAccount ().add (aEbiAccount);
          aEbiInvoice.setPaymentMethod (aEbiUBTMethod);
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
