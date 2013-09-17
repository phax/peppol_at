package at.gv.brz.transform.ubl2ebi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.DeliveryType;
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
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PeriodType;
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
import at.gv.brz.transform.ubl2ebi.helper.SchemedID;
import at.gv.brz.transform.ubl2ebi.helper.TaxCategoryKey;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.error.EErrorLevel;
import com.phloc.commons.locale.country.CountryCache;
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
import com.phloc.ebinterface.v40.Ebi40DeliveryType;
import com.phloc.ebinterface.v40.Ebi40DetailsType;
import com.phloc.ebinterface.v40.Ebi40DocumentTypeType;
import com.phloc.ebinterface.v40.Ebi40InvoiceRecipientType;
import com.phloc.ebinterface.v40.Ebi40InvoiceType;
import com.phloc.ebinterface.v40.Ebi40ItemListType;
import com.phloc.ebinterface.v40.Ebi40ItemType;
import com.phloc.ebinterface.v40.Ebi40ListLineItemType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceDetailType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceType;
import com.phloc.ebinterface.v40.Ebi40PeriodType;
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
import com.phloc.validation.error.ErrorList;

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
  private static final String REGEX_BIC = "[0-9A-Za-z]{8}([0-9A-Za-z]{3})?";
  private static final String SUPPORTED_TAX_SCHEME_SCHEME_ID = "UN/ECE 5153";
  private static final int IBAN_MAX_LENGTH = 34;
  private static final String PAYMENT_CHANNEL_CODE_IBAN = "IBAN";
  private static final String SUPPORTED_TAX_SCHEME_ID = "VAT";
  private static final String EBI_GENERATING_SYSTEM = "UBL 2.0 to ebInterface 4.0 converter";
  private static final int SCALE_PERC = 2;
  private static final int SCALE_PRICE_LINE = 4;
  // Austria uses HALF_UP mode!
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  @SuppressWarnings ("unused")
  private final Locale m_aDisplayLocale;
  private final boolean m_bStrictERBMode;

  public PEPPOLUBL20ToEbInterface40Converter (@Nonnull final Locale aDisplayLocale, final boolean bStrictERBMode) {
    if (aDisplayLocale == null)
      throw new NullPointerException ("DisplayLocale");

    m_aDisplayLocale = aDisplayLocale;
    m_bStrictERBMode = bStrictERBMode;
  }

  /**
   * Check if the passed UBL invoice is transformable
   * 
   * @param aUBLInvoice
   *        The UBL invoice to check
   */
  private static void _checkConsistency (@Nonnull final InvoiceType aUBLInvoice,
                                         @Nonnull final ErrorList aTransformationErrorList) {
    // Check UBLVersionID
    final UBLVersionIDType aUBLVersionID = aUBLInvoice.getUBLVersionID ();
    if (aUBLVersionID == null)
      aTransformationErrorList.addError ("UBLVersionID", "No UBLVersionID present!");
    else
      if (!CPeppolUBL.UBL_VERSION.equals (aUBLVersionID.getValue ()))
        aTransformationErrorList.addError ("UBLVersionID", "Invalid UBLVersionID value '" +
                                                           aUBLVersionID.getValue () +
                                                           "' present!");

    // Check ProfileID
    IPeppolPredefinedProcessIdentifier aProcID = null;
    final ProfileIDType aProfileID = aUBLInvoice.getProfileID ();
    if (aProfileID == null)
      aTransformationErrorList.addError ("ProfileID", "No ProfileID present!");
    else {
      final String sProfileID = StringHelper.trim (aProfileID.getValue ());
      aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
      if (aProcID == null)
        aTransformationErrorList.addError ("ProfileID", "Invalid ProfileID value '" + sProfileID + "' present!");
    }

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false) {
      final CustomizationIDType aCustomizationID = aUBLInvoice.getCustomizationID ();
      if (aCustomizationID == null)
        aTransformationErrorList.addError ("CustomizationID", "No CustomizationID present!");
      else
        if (!CPeppolUBL.CUSTOMIZATION_SCHEMEID.equals (aCustomizationID.getSchemeID ()))
          aTransformationErrorList.addError ("CustomizationID",
                                             "Invalid CustomizationID schemeID '" +
                                                 aCustomizationID.getSchemeID () +
                                                 "' present. Must be '" +
                                                 CPeppolUBL.CUSTOMIZATION_SCHEMEID +
                                                 "'");
        else
          if (aProcID != null) {
            final String sCustomizationID = aCustomizationID.getValue ();
            IPeppolPredefinedDocumentTypeIdentifier aMatchingDocID = null;
            for (final IPeppolPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
              if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID)) {
                // We found a match
                aMatchingDocID = aDocID;
                break;
              }
            if (aMatchingDocID == null)
              aTransformationErrorList.addError ("CustomizationID", "Invalid CustomizationID value '" +
                                                                    sCustomizationID +
                                                                    "' present! It is not supported by the passed profile.");
          }
    }

    // Invoice type code
    final InvoiceTypeCodeType aInvoiceTypeCode = aUBLInvoice.getInvoiceTypeCode ();
    if (aInvoiceTypeCode == null) {
      // None present
      aTransformationErrorList.addWarning ("InvoiceTypeCode", "No InvoiceTypeCode present! Assuming " +
                                                              CPeppolUBL.INVOICE_TYPE_CODE);
    }
    else {
      // If one is present, it must match
      final String sInvoiceTypeCode = aInvoiceTypeCode.getValue ().trim ();
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (sInvoiceTypeCode))
        aTransformationErrorList.addError ("InvoiceTypeCode",
                                           "Invalid InvoiceTypeCode value '" +
                                               sInvoiceTypeCode.trim () +
                                               "' present! Expected '" +
                                               CPeppolUBL.INVOICE_TYPE_CODE +
                                               "'");
    }
  }

  private static void _setAddressData (@Nullable final AddressType aUBLAddress,
                                       @Nonnull final Ebi40AddressType aEbiAddress,
                                       @Nonnull final String sPartyType,
                                       @Nonnull final ErrorList aTransformationErrorList) {
    // Convert main address
    if (aUBLAddress != null) {
      aEbiAddress.setStreet (StringHelper.getImplodedNonEmpty (" ",
                                                               aUBLAddress.getStreetNameValue (),
                                                               aUBLAddress.getBuildingNumberValue ()));
      aEbiAddress.setPOBox (aUBLAddress.getPostboxValue ());
      aEbiAddress.setTown (aUBLAddress.getCityNameValue ());
      aEbiAddress.setZIP (aUBLAddress.getPostalZoneValue ());

      // Country
      if (aUBLAddress.getCountry () != null) {
        final Ebi40CountryType aEbiCountry = new Ebi40CountryType ();
        Ebi40CountryCodeType aCC = null;
        try {
          aCC = Ebi40CountryCodeType.fromValue (aUBLAddress.getCountry ().getIdentificationCodeValue ());
        }
        catch (final IllegalArgumentException ex) {}
        aEbiCountry.setCountryCode (aCC);
        aEbiCountry.setContent (aUBLAddress.getCountry ().getNameValue ());
        if (StringHelper.hasNoText (aEbiCountry.getContent ()) && aCC != null) {
          final Locale aLocale = CountryCache.getCountry (aCC.value ());
          aEbiCountry.setContent (aLocale.getDisplayCountry ());
        }
        aEbiAddress.setCountry (aEbiCountry);
      }
    }

    if (aEbiAddress.getStreet () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/StreetName", "Address is missing a street name");
    if (aEbiAddress.getTown () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/CityName", "Address is missing a town name");
    if (aEbiAddress.getZIP () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/PostalZone", "Address is missing a ZIP code");
    if (aEbiAddress.getCountry () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/Country/IdentificationCode",
                                         "Address is missing a country");
  }

  @Nonnull
  private static Ebi40AddressType _convertParty (@Nonnull final PartyType aUBLParty,
                                                 @Nonnull final String sPartyType,
                                                 @Nonnull final ErrorList aTransformationErrorList) {
    final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      aEbiAddress.setName (aUBLPartyName.getNameValue ());
      if (aUBLParty.getPartyNameCount () > 1)
        aTransformationErrorList.addWarning (sPartyType + "/PartyName",
                                             "Multiple party names present - only the first one is used!");
      if (aEbiAddress.getName () == null)
        aTransformationErrorList.addError (sPartyType, "Party name is missing!");
    }

    // Convert main address
    _setAddressData (aUBLParty.getPostalAddress (), aEbiAddress, sPartyType, aTransformationErrorList);

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
      final String sEndpointID = StringHelper.trim (aUBLParty.getEndpointIDValue ());
      if (StringHelper.hasText (sEndpointID)) {
        // We have an endpoint ID

        // Check all identifier types
        final String sSchemeIDToSearch = aUBLParty.getEndpointID ().getSchemeID ();

        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (sSchemeIDToSearch)) {
            final Ebi40AddressIdentifierType aEbiType = new Ebi40AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setContent (sEndpointID);
            aEbiAddress.setAddressIdentifier (aEbiType);
            break;
          }

        if (aEbiAddress.getAddressIdentifier () == null)
          aTransformationErrorList.addWarning (sPartyType, "Ignoring endpoint ID '" +
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
        aTransformationErrorList.addWarning (sPartyType + "/PartyIdentification", "Ignoring identification '" +
                                                                                  sUBLPartyID +
                                                                                  "' of type '" +
                                                                                  aUBLPartyID.getID ().getSchemeID () +
                                                                                  "'");
      }
    }

    return aEbiAddress;
  }

  private static boolean _isSupportedTaxSchemeSchemeID (@Nullable final String sUBLTaxSchemeSchemeID) {
    return sUBLTaxSchemeSchemeID == null ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID) ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID + " Subset");
  }

  @Nullable
  private static String _makeAlphaNumIDType (@Nullable final String sText,
                                             @Nonnull final String sContext,
                                             @Nonnull final ErrorList aTransformationErrorList) {
    if (sText != null && !RegExHelper.stringMatchesPattern ("[0-9 | A-Z | a-z | -_äöüÄÖÜß]+", sText)) {
      final String ret = RegExHelper.stringReplacePattern ("[^0-9 | A-Z | a-z | -_äöüÄÖÜß]", sText, "_");
      aTransformationErrorList.addWarning (sContext, "'" +
                                                     sText +
                                                     "' is not an AlphaNumIDType and was changed to '" +
                                                     ret +
                                                     "'!");
      return ret;
    }
    return sText;
  }

  /**
   * Main conversion method to convert from UBL 2.0 to ebInterface 3.0.2
   * 
   * @param aUBLInvoice
   *        The UBL invoice to be converted
   * @param aTransformationErrorList
   *        Error list. Must be empty!
   * @return The created ebInterface 4.0 document or <code>null</code> in case
   *         of a severe error.
   */
  @Nullable
  public Ebi40InvoiceType convertToEbInterface (@Nonnull final InvoiceType aUBLInvoice,
                                                @Nonnull final ErrorList aTransformationErrorList) {
    if (aUBLInvoice == null)
      throw new NullPointerException ("UBLInvoice");
    if (aTransformationErrorList == null)
      throw new NullPointerException ("TransformationErrorList");
    if (!aTransformationErrorList.isEmpty ())
      throw new IllegalArgumentException ("TransformationErrorList must be empty!");

    // Consistency check before starting the conversion
    _checkConsistency (aUBLInvoice, aTransformationErrorList);
    if (!aTransformationErrorList.isEmpty () &&
        aTransformationErrorList.getMostSevereErrorLevel ().isMoreOrEqualSevereThan (EErrorLevel.ERROR))
      return null;

    // Build ebInterface invoice
    final Ebi40InvoiceType aEbiInvoice = new Ebi40InvoiceType ();
    aEbiInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiInvoice.setDocumentType (Ebi40DocumentTypeType.INVOICE);
    aEbiInvoice.setInvoiceCurrency (Ebi40CurrencyType.fromValue (StringHelper.trim (aUBLInvoice.getDocumentCurrencyCodeValue ())));
    aEbiInvoice.setInvoiceNumber (aUBLInvoice.getIDValue ());
    aEbiInvoice.setInvoiceDate (aUBLInvoice.getIssueDateValue ());

    // Biller/Supplier (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLInvoice.getAccountingSupplierParty ();
      final Ebi40BillerType aEbiBiller = new Ebi40BillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ().equals (SUPPORTED_TAX_SCHEME_ID)) {
          aEbiBiller.setVATIdentificationNumber (aUBLPartyTaxScheme.getCompanyIDValue ());
          break;
        }
      if (StringHelper.hasNoText (aEbiBiller.getVATIdentificationNumber ())) {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingSupplierParty/Party/PartyTaxScheme",
                                           "Failed to get biller VAT number!");
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null) {
        // The customer's internal identifier for the supplier.
        aEbiBiller.setInvoiceRecipientsBillerID (aUBLSupplier.getCustomerAssignedAccountID ().getValue ());
      }
      if (StringHelper.hasNoText (aEbiBiller.getInvoiceRecipientsBillerID ())) {
        if (m_bStrictERBMode) {
          // Mandatory field
          aTransformationErrorList.addError ("AccountingSupplierParty/CustomerAssignedAccountID",
                                             "Failed to get customer assigned account ID for supplier!");
        }
      }
      aEbiBiller.setAddress (_convertParty (aUBLSupplier.getParty (),
                                            "AccountingSupplierParty",
                                            aTransformationErrorList));
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
      if (StringHelper.hasNoText (aEbiRecipient.getVATIdentificationNumber ())) {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingCustomerParty/PartyTaxScheme",
                                           "Failed to get supplier VAT number!");
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null) {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        aEbiRecipient.setBillersInvoiceRecipientID (aUBLCustomer.getSupplierAssignedAccountIDValue ());
      }
      if (StringHelper.hasNoText (aEbiRecipient.getBillersInvoiceRecipientID ())) {
        // Mandatory field
        aTransformationErrorList.addWarning ("AccountingCustomerParty/SupplierAssignedAccountID",
                                             "Failed to get supplier assigned account ID for customer! Defaulting to 000!");
        aEbiRecipient.setBillersInvoiceRecipientID ("000");
      }
      aEbiRecipient.setAddress (_convertParty (aUBLCustomer.getParty (),
                                               "AccountingCustomerParty",
                                               aTransformationErrorList));
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
        aTransformationErrorList.addError ("OrderReference/ID", "Failed to get order reference ID!");
      }
      else {
        if (sUBLOrderReferenceID != null && sUBLOrderReferenceID.length () > 35) {
          aTransformationErrorList.addWarning ("OrderReference/ID", "Order reference value '" +
                                                                    sUBLOrderReferenceID +
                                                                    "' is too long. It will be cut to 35 characters.");
          sUBLOrderReferenceID = sUBLOrderReferenceID.substring (0, 35);
        }

        sUBLOrderReferenceID = _makeAlphaNumIDType (sUBLOrderReferenceID, "OrderReference/ID", aTransformationErrorList);
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
            aTransformationErrorList.addError ("TaxTotal/TaxSubtotal/TaxCategory/",
                                               "Other tax scheme found and ignored: '" +
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
          aTransformationErrorList.addWarning ("TaxTotal/TaxSubtotal/TaxCategory",
                                               "Failed to resolve tax percentage for invoice line! Defaulting to 0%.");
          aUBLPercent = BigDecimal.ZERO;
        }

        // Start creating ebInterface line
        final Ebi40ListLineItemType aEbiListLineItem = new Ebi40ListLineItemType ();

        // Invoice line number
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (aUBLInvoiceLine.getIDValue ());
        if (aUBLPositionNumber == null) {
          aTransformationErrorList.addWarning ("InvoiceLine/ID",
                                               "Failed to parse UBL invoice line '" +
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
          aEbiListLineItem.setUnitPrice (aUBLBaseQuantity == null
                                                                 ? aUBLPriceAmount
                                                                 : MathHelper.isEqualToZero (aUBLBaseQuantity)
                                                                                                              ? BigDecimal.ZERO
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
        if (EqualsUtils.equals (aUBLPercent, BigDecimal.ZERO))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Order reference for detail
        final OrderLineReferenceType aOrderLineReference = ContainerHelper.getFirstElement (aUBLInvoiceLine.getOrderLineReference ());
        if (aOrderLineReference != null) {
          final Ebi40OrderReferenceDetailType aEbiOrderReferenceDetail = new Ebi40OrderReferenceDetailType ();
          aEbiOrderReferenceDetail.setOrderID (sUBLOrderReferenceID);
          aEbiOrderReferenceDetail.setOrderPositionNumber (aOrderLineReference.getLineIDValue ());
          aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderReferenceDetail);
        }

        // Invoice recipients order reference
        for (final OrderLineReferenceType aUBLOrderLineReference : aUBLInvoiceLine.getOrderLineReference ())
          if (StringHelper.hasText (aUBLOrderLineReference.getLineIDValue ())) {
            final Ebi40OrderReferenceDetailType aEbiOrderRefDetail = new Ebi40OrderReferenceDetailType ();
            aEbiOrderRefDetail.setOrderID (sUBLOrderReferenceID);
            aEbiOrderRefDetail.setOrderPositionNumber (aUBLOrderLineReference.getLineIDValue ());
            aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderRefDetail);
            break;
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
              aTransformationErrorList.addWarning ("InvoiceLine/AllowanceCharge",
                                                   "Reduction and surcharge is mixed in this invoice! This might not be supported by all ebInterface 4.0 interpreters.");

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
      aTransformationErrorList.addError ("InvoiceLine", "No single VAT item found.");
      if (false) {
        // No default in this case
        final Ebi40ItemType aEbiVATItem = new Ebi40ItemType ();
        aEbiVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
        final Ebi40TaxRateType aEbiVATTaxRate = new Ebi40TaxRateType ();
        aEbiVATTaxRate.setValue (BigDecimal.ZERO);
        aEbiVATItem.setTaxRate (aEbiVATTaxRate);
        aEbiVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
        aEbiVAT.getItem ().add (aEbiVATItem);
      }
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
          aTransformationErrorList.addWarning ("Invoice/AllowanceCharge",
                                               "Reduction and surcharge is mixed in this invoice! This might not be supported by all ebInterface 4.0 interpreters.");

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
          aTransformationErrorList.addError ("Invoice/AllowanceCharge",
                                             "Failed to resolve tax percentage for global AllowanceCharge!");
          if (false) {
            aEbiTaxRate = new Ebi40TaxRateType ();
            aEbiTaxRate.setValue (BigDecimal.ZERO);
            aEbiTaxRate.setTaxCode (ETaxCode.NOT_TAXABLE.getID ());
          }
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
              aTransformationErrorList.addError ("PaymentMeans/PayeeFinancialAccount/FinancialInstitutionBranch/FinancialInstitution/ID",
                                                 "The BIC '" +
                                                     aEbiAccount.getBIC () +
                                                     "' does not match the required regular expression.");
              aEbiAccount.setBIC (null);
            }
          }

          // IBAN
          aEbiAccount.setIBAN (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue ());
          if (StringHelper.getLength (aEbiAccount.getIBAN ()) > IBAN_MAX_LENGTH) {
            aTransformationErrorList.addWarning ("PaymentMeans/PayeeFinancialAccount/ID",
                                                 "The IBAN '" +
                                                     aEbiAccount.getIBAN () +
                                                     "' is too long and cut to " +
                                                     IBAN_MAX_LENGTH +
                                                     " chars.");
            aEbiAccount.setIBAN (aEbiAccount.getIBAN ().substring (0, IBAN_MAX_LENGTH));
          }

          // Bank Account Owner - no field present - check PayeePart or
          // SupplierPartyName
          String sBankAccountOwnerName = null;
          if (aUBLInvoice.getPayeeParty () != null && aUBLInvoice.getPayeeParty ().getPartyNameCount () > 0) {
            sBankAccountOwnerName = aUBLInvoice.getPayeeParty ().getPartyNameAtIndex (0).getNameValue ();
          }
          if (StringHelper.hasNoText (sBankAccountOwnerName)) {
            final PartyType aParty = aUBLInvoice.getAccountingSupplierParty ().getParty ();
            if (aParty != null && aParty.getPartyNameCount () > 0)
              sBankAccountOwnerName = aParty.getPartyNameAtIndex (0).getNameValue ();
          }
          if (StringHelper.hasNoText (sBankAccountOwnerName)) {
            if (m_bStrictERBMode) {
              aTransformationErrorList.addError ("PayeeParty/PartyName/Name",
                                                 "Failed to determine the bank account owner name.");
            }
          }
          aEbiAccount.setBankAccountOwner (sBankAccountOwnerName);

          aEbiUBTMethod.getBeneficiaryAccount ().add (aEbiAccount);
          aEbiInvoice.setPaymentMethod (aEbiUBTMethod);
          break;
        }
      }
    }

    // Delivery
    final Ebi40DeliveryType aEbiDelivery = new Ebi40DeliveryType ();
    if (aUBLInvoice.getDeliveryCount () > 0) {
      final DeliveryType aUBLDelivery = aUBLInvoice.getDeliveryAtIndex (0);

      // Delivery data
      if (aUBLDelivery.getActualDeliveryDate () != null)
        aEbiDelivery.setDate (aUBLDelivery.getActualDeliveryDateValue ());

      // Address
      if (aUBLDelivery.getDeliveryLocation () != null && aUBLDelivery.getDeliveryLocation ().getAddress () != null) {
        final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();
        _setAddressData (aUBLDelivery.getDeliveryLocation ().getAddress (),
                         aEbiAddress,
                         "Delivery",
                         aTransformationErrorList);

        // Check delivery party
        if (aUBLDelivery.getDeliveryParty () != null && aUBLDelivery.getDeliveryParty ().hasPartyNameEntries ())
          aEbiAddress.setName (aUBLDelivery.getDeliveryParty ().getPartyNameAtIndex (0).getNameValue ());

        // As fallback use accounting customer party
        if (StringHelper.hasNoText (aEbiAddress.getName ()))
          if (aUBLInvoice.getAccountingCustomerParty () != null &&
              aUBLInvoice.getAccountingCustomerParty ().getParty () != null &&
              aUBLInvoice.getAccountingCustomerParty ().getParty ().hasPartyNameEntries ()) {
            aEbiAddress.setName (aUBLInvoice.getAccountingCustomerParty ()
                                            .getParty ()
                                            .getPartyNameAtIndex (0)
                                            .getNameValue ());
          }

        if (StringHelper.hasNoText (aEbiAddress.getName ()))
          aTransformationErrorList.addError ("Delivery/DeliveryParty",
                                             "If a Delivery/DeliveryLocation/Address is present, a Delivery/DeliveryParty/PartyName/Name must also be present!");

        aEbiDelivery.setAddress (aEbiAddress);
      }
    }

    if (aEbiDelivery.getDate () == null) {
      // Check for service period
      final PeriodType aUBLInvoicePeriod = ContainerHelper.getSafe (aUBLInvoice.getInvoicePeriod (), 0);
      if (aUBLInvoicePeriod != null) {
        final Ebi40PeriodType aEbiPeriod = new Ebi40PeriodType ();
        aEbiPeriod.setFromDate (aUBLInvoicePeriod.getStartDateValue ());
        aEbiPeriod.setToDate (aUBLInvoicePeriod.getEndDateValue ());
        aEbiDelivery.setPeriod (aEbiPeriod);
      }
    }

    if (m_bStrictERBMode)
      if (aEbiDelivery.getDate () == null && aEbiDelivery.getPeriod () == null)
        aTransformationErrorList.addError ("Invoice", "A Delivery/DeliveryDate or an InvoicePeriod must be present!");

    aEbiInvoice.setDelivery (aEbiDelivery);

    return aEbiInvoice;
  }
}
