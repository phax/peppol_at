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
import com.phloc.commons.annotations.Translatable;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.locale.country.CountryCache;
import com.phloc.commons.math.MathHelper;
import com.phloc.commons.name.IHasDisplayText;
import com.phloc.commons.name.IHasDisplayTextWithArgs;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.state.ETriState;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.commons.text.ITextProvider;
import com.phloc.commons.text.impl.TextProvider;
import com.phloc.commons.text.resolve.DefaultTextResolver;
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
  public static final String DEFAULT_BILLERS_INVOICERECIPIENT_ID = "000";

  @Translatable
  public static enum EText implements IHasDisplayText, IHasDisplayTextWithArgs {
    NO_UBL_VERSION_ID ("Die UBLVersionID fehlt. Es wird der Wert ''{0}'' erwartet.",
                       "No UBLVersionID present. It must be ''{0}''."),
    INVALID_UBL_VERSION_ID ("Die UBLVersionID ''{0}'' ist ungültig. Diese muss den Wert ''{1}'' haben.",
                            "Invalid UBLVersionID value ''{0}'' present. It must be ''{1}''."),
    NO_PROFILE_ID ("Die ProfileID fehlt", "No ProfileID present."),
    INVALID_PROFILE_ID ("Die ProfileID ''{0}'' ist ungültig.", "Invalid ProfileID value ''{0}'' present."),
    NO_CUSTOMIZATION_ID ("Die CustomizationID fehlt", "No CustomizationID present."),
    INVALID_CUSTOMIZATION_SCHEME_ID ("Die CustomizationID schemeID ''{0}'' ist ungültig. Diese muss den Wert ''{1}'' haben.",
                                     "Invalid CustomizationID schemeID value ''{0}'' present. It must be ''{1}''."),
    INVALID_CUSTOMIZATION_ID ("Die angegebene CustomizationID ''{0}'' ist ungültig. Sie wird vom angegebenen Profil nicht unterstützt.",
                              "Invalid CustomizationID value ''{0}'' present. It is not supported by the passed profile."),
    NO_INVOICE_TYPECODE ("Der InvoiceTypeCode fehlt. Es wird der Wert ''{0}'' erwartet.",
                         "No InvoiceTypeCode present. It must be ''{0}''."),
    INVALID_INVOICE_TYPECODE ("Der InvoiceTypeCode ''{0}'' ist ungültig. Dieser muss den Wert ''{1}'' haben.",
                              "Invalid InvoiceTypeCode value ''{0}'' present. It must be ''{1}''."),
    ADDRESS_NO_STREET ("In der Adresse fehlt die Straße.", "Address is missing a street name."),
    ADDRESS_NO_CITY ("In der Adresse fehlt der Name der Stadt.", "Address is missing a city name."),
    ADDRESS_NO_ZIPCODE ("In der Adresse fehlt die PLZ.", "Address is missing a ZIP code."),
    ADDRESS_NO_COUNTRY ("In der Adresse fehlt der Name des Landes.", "Address is missing a country."),
    MULTIPLE_PARTIES ("Es sind mehrere Partynamen vorhanden - nur der erste wird verwendet.",
                      "Multiple party names present - only the first one is used."),
    PARTY_NO_NAME ("Der Name der Party fehlt.", "Party name is missing."),
    PARTY_UNSUPPORTED_ENDPOINT ("Ignoriere den Enpunkt ''{0}'' des Typs ''{1}''.",
                                "Ignoring endpoint ID ''{0}'' of type ''{1}''."),
    PARTY_UNSUPPORTED_ADDRESS_IDENTIFIER ("Ignoriere die ID ''{0}'' des Typs ''{1}''.",
                                          "Ignoring identification ''{0}'' of type ''{1}''."),
    ALPHANUM_ID_TYPE_CHANGE ("''{0}'' ist ein ungültiger Typ und wurde auf ''{1}'' geändert.",
                             "''{0}'' is an invalid value and was changed to ''{1}''."),
    INVALID_CURRENCY_CODE ("Der angegebene Währungscode ''{0}'' ist ungültig.",
                           "Invalid currency code ''{0}'' provided."),
    BILLER_VAT_MISSING ("Die UID-Nummer des Rechnungsstellers fehlt. Verwenden Sie 'ATU00000000' für österreichische Rechnungssteller an wenn keine UID-Nummer notwendig ist.",
                        "Failed to get biller VAT identification number. Use 'ATU00000000' for Austrian invoice recipients if no VAT identification number is required."),
    CUSTOMER_ASSIGNED_ACCOUNTID_MISSING ("Die ID des Rechnungsstellers beim Rechnungsempfänger fehlt.",
                                         "Failed to get customer assigned account ID for supplier."),
    SUPPLIER_VAT_MISSING ("Die UID-Nummer des Rechnungsempfängers fehlt. Verwenden Sie 'ATU00000000' für österreichische Empfänger an wenn keine UID-Nummer notwendig ist.",
                          "Failed to get supplier VAT identification number. Use 'ATU00000000' for Austrian invoice recipients if no VAT identification number is required."),
    SUPPLIER_ASSIGNED_ACCOUNTID_MISSING ("Die ID des Rechnungsempfängers beim Rechnungssteller fehlt. Der Standardwert ''{0}'' wird verwendet.",
                                         "Failed to get supplier assigned account ID for customer. Defaulting to ''{0}''."),
    ORDER_REFERENCE_MISSING ("Die Auftragsreferenz fehlt.", "Failed to get order reference ID."),
    ORDER_REFERENCE_TOO_LONG ("Die Auftragsreferenz ''{0}'' ist zu lang und wurde nach {1} Zeichen abgeschnitten.",
                              "Order reference value ''{0}'' is too long and was cut to {1} characters."),
    UNSUPPORTED_TAX_SCHEME ("Nicht unterstütztes Steuerschema gefunden: ''{0}'' und ''{1}''.",
                            "Other tax scheme found and ignored: ''{0}'' and ''{1}''."),
    DETAILS_TAX_PERCENTAGE_NOT_FOUND ("Der Steuersatz der Rechnungszeile konnte nicht ermittelt werden. Verwende den Standardwert {0}%.",
                                      "Failed to resolve tax percentage for invoice line. Defaulting to {0}%."),
    DETAILS_INVALID_POSITION ("Die Rechnungspositionsnummer ''{0}'' ist nicht numerisch. Es wird der Index {1} verwendet.",
                              "The UBL invoice line ID ''{0}'' is not numeric. Defaulting to index {1}."),
    DETAILS_INVALID_UNIT ("Die Rechnungszeile hat keine Mengeneinheit. Verwende den Standardwert ''{0}''.",
                          "The UBL invoice line has no unit of measure. Defaulting to ''{0}''."),
    DETAILS_INVALID_QUANTITY ("Die Rechnungszeile hat keine Menge. Verwende den Standardwert ''{0}''.",
                              "The UBL invoice line has no quantity. Defaulting to ''{0}''."),
    VAT_ITEM_MISSING ("Keine einzige Steuersumme gefunden", "No single VAT item found."),
    ALLOWANCE_CHARGE_NO_TAXRATE ("Die Steuerprozentrate für den globalen Zuschlag/Abschlag konnte nicht ermittelt werden.",
                                 "Failed to resolve tax rate percentage for global AllowanceCharge."),
    BIC_INVALID ("Der BIC ''{0}'' ist ungültig.", "The BIC ''{0}'' is invalid."),
    IBAN_TOO_LONG ("Der IBAN ''{0}'' ist zu lang. Er wurde nach {1} Zeichen abgeschnitten.",
                   "The IBAN ''{0}'' is too long and was cut to {1} characters."),
    DELIVERY_WITHOUT_NAME ("Wenn eine Delivery/DeliveryLocation/Address angegeben ist muss auch ein Delivery/DeliveryParty/PartyName/Name angegeben werden.",
                           "If a Delivery/DeliveryLocation/Address is present, a Delivery/DeliveryParty/PartyName/Name must also be present."),
    NO_DELIVERY_DATE ("Ein Lieferdatum oder ein Leistungszeitraum muss vorhanden sein.",
                      "A Delivery/DeliveryDate or an InvoicePeriod must be present.");

    private final ITextProvider m_aTP;

    private EText (@Nonnull final String sDE, @Nonnull final String sEN) {
      m_aTP = TextProvider.create_DE_EN (sDE, sEN);
    }

    @Nullable
    public String getDisplayText (@Nonnull final Locale aContentLocale) {
      return DefaultTextResolver.getText (this, m_aTP, aContentLocale);
    }

    @Nullable
    public String getDisplayTextWithArgs (@Nonnull final Locale aContentLocale, @Nullable final Object... aArgs) {
      return DefaultTextResolver.getTextWithArgs (this, m_aTP, aContentLocale, aArgs);
    }
  }

  public static final int ORDER_REFERENCE_MAX_LENGTH = 35;
  public static final String REGEX_BIC = "[0-9A-Za-z]{8}([0-9A-Za-z]{3})?";
  public static final String SUPPORTED_TAX_SCHEME_SCHEME_ID = "UN/ECE 5153";
  public static final String SUPPORTED_TAX_SCHEME_SCHEME_ID_SUBSET = SUPPORTED_TAX_SCHEME_SCHEME_ID + " Subset";
  public static final int IBAN_MAX_LENGTH = 34;
  public static final String PAYMENT_CHANNEL_CODE_IBAN = "IBAN";
  public static final String SUPPORTED_TAX_SCHEME_ID = "VAT";
  public static final String EBI_GENERATING_SYSTEM = "UBL 2.0 to ebInterface 4.0 converter";
  public static final int SCALE_PERC = 2;
  public static final int SCALE_PRICE_LINE = 4;
  // Austria uses HALF_UP mode!
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

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
  private void _checkConsistency (@Nonnull final InvoiceType aUBLInvoice,
                                  @Nonnull final ErrorList aTransformationErrorList) {
    // Check UBLVersionID
    final UBLVersionIDType aUBLVersionID = aUBLInvoice.getUBLVersionID ();
    if (aUBLVersionID == null)
      aTransformationErrorList.addError ("UBLVersionID",
                                         EText.NO_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                         CPeppolUBL.UBL_VERSION));
    else
      if (!CPeppolUBL.UBL_VERSION.equals (aUBLVersionID.getValue ()))
        aTransformationErrorList.addError ("UBLVersionID",
                                           EText.INVALID_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                aUBLVersionID.getValue (),
                                                                                                CPeppolUBL.UBL_VERSION));

    // Check ProfileID
    IPeppolPredefinedProcessIdentifier aProcID = null;
    final ProfileIDType aProfileID = aUBLInvoice.getProfileID ();
    if (aProfileID == null)
      aTransformationErrorList.addError ("ProfileID", EText.NO_PROFILE_ID.getDisplayText (m_aDisplayLocale));
    else {
      final String sProfileID = StringHelper.trim (aProfileID.getValue ());
      aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
      if (aProcID == null)
        aTransformationErrorList.addError ("ProfileID",
                                           EText.INVALID_PROFILE_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                            sProfileID));
    }

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false) {
      final CustomizationIDType aCustomizationID = aUBLInvoice.getCustomizationID ();
      if (aCustomizationID == null)
        aTransformationErrorList.addError ("CustomizationID",
                                           EText.NO_CUSTOMIZATION_ID.getDisplayText (m_aDisplayLocale));
      else
        if (!CPeppolUBL.CUSTOMIZATION_SCHEMEID.equals (aCustomizationID.getSchemeID ()))
          aTransformationErrorList.addError ("CustomizationID/schemeID",
                                             EText.INVALID_CUSTOMIZATION_SCHEME_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                           aCustomizationID.getSchemeID (),
                                                                                                           CPeppolUBL.CUSTOMIZATION_SCHEMEID));
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
              aTransformationErrorList.addError ("CustomizationID",
                                                 EText.INVALID_CUSTOMIZATION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                        sCustomizationID));
          }
    }

    // Invoice type code
    final InvoiceTypeCodeType aInvoiceTypeCode = aUBLInvoice.getInvoiceTypeCode ();
    if (aInvoiceTypeCode == null) {
      // None present
      aTransformationErrorList.addWarning ("InvoiceTypeCode",
                                           EText.NO_INVOICE_TYPECODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             CPeppolUBL.INVOICE_TYPE_CODE));
    }
    else {
      // If one is present, it must match
      final String sInvoiceTypeCode = aInvoiceTypeCode.getValue ().trim ();
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (sInvoiceTypeCode))
        aTransformationErrorList.addError ("InvoiceTypeCode",
                                           EText.INVALID_INVOICE_TYPECODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  sInvoiceTypeCode,
                                                                                                  CPeppolUBL.INVOICE_TYPE_CODE));
    }
  }

  private void _setAddressData (@Nullable final AddressType aUBLAddress,
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
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/StreetName",
                                         EText.ADDRESS_NO_STREET.getDisplayText (m_aDisplayLocale));
    if (aEbiAddress.getTown () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/CityName",
                                         EText.ADDRESS_NO_CITY.getDisplayText (m_aDisplayLocale));
    if (aEbiAddress.getZIP () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/PostalZone",
                                         EText.ADDRESS_NO_ZIPCODE.getDisplayText (m_aDisplayLocale));
    if (aEbiAddress.getCountry () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/Country/IdentificationCode",
                                         EText.ADDRESS_NO_COUNTRY.getDisplayText (m_aDisplayLocale));
  }

  @Nonnull
  private Ebi40AddressType _convertParty (@Nonnull final PartyType aUBLParty,
                                          @Nonnull final String sPartyType,
                                          @Nonnull final ErrorList aTransformationErrorList) {
    final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      aEbiAddress.setName (aUBLPartyName.getNameValue ());
      if (aUBLParty.getPartyNameCount () > 1)
        aTransformationErrorList.addWarning (sPartyType + "/PartyName",
                                             EText.MULTIPLE_PARTIES.getDisplayText (m_aDisplayLocale));
      if (aEbiAddress.getName () == null)
        aTransformationErrorList.addError (sPartyType, EText.PARTY_NO_NAME.getDisplayText (m_aDisplayLocale));
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
          aTransformationErrorList.addWarning (sPartyType,
                                               EText.PARTY_UNSUPPORTED_ENDPOINT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                        sEndpointID,
                                                                                                        aUBLParty.getEndpointID ()
                                                                                                                 .getSchemeID ()));
      }
    }

    if (aEbiAddress.getAddressIdentifier () == null) {
      // check party identification
      outer: for (final PartyIdentificationType aUBLPartyID : aUBLParty.getPartyIdentification ()) {
        final String sUBLPartyID = StringHelper.trim (aUBLPartyID.getIDValue ());
        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (aUBLPartyID.getID ().getSchemeID ())) {
            // Add GLN/DUNS number
            final Ebi40AddressIdentifierType aEbiType = new Ebi40AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setContent (sUBLPartyID);
            aEbiAddress.setAddressIdentifier (aEbiType);
            break outer;
          }
        aTransformationErrorList.addWarning (sPartyType + "/PartyIdentification",
                                             EText.PARTY_UNSUPPORTED_ADDRESS_IDENTIFIER.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                                sUBLPartyID,
                                                                                                                aUBLPartyID.getID ()
                                                                                                                           .getSchemeID ()));
      }
    }

    return aEbiAddress;
  }

  private static boolean _isSupportedTaxSchemeSchemeID (@Nullable final String sUBLTaxSchemeSchemeID) {
    return sUBLTaxSchemeSchemeID == null ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID) ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID_SUBSET);
  }

  @Nullable
  private String _makeAlphaNumIDType (@Nullable final String sText,
                                      @Nonnull final String sContext,
                                      @Nonnull final ErrorList aTransformationErrorList) {
    if (sText != null && !RegExHelper.stringMatchesPattern ("[0-9 | A-Z | a-z | -_äöüÄÖÜß]+", sText)) {
      final String ret = RegExHelper.stringReplacePattern ("[^0-9 | A-Z | a-z | -_äöüÄÖÜß]", sText, "_");
      aTransformationErrorList.addWarning (sContext,
                                           EText.ALPHANUM_ID_TYPE_CHANGE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                 sText,
                                                                                                 ret));
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
    if (aTransformationErrorList.containsAtLeastOneError ())
      return null;

    // Build ebInterface invoice
    final Ebi40InvoiceType aEbiInvoice = new Ebi40InvoiceType ();
    aEbiInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiInvoice.setDocumentType (Ebi40DocumentTypeType.INVOICE);

    final String sUBLCurrencyCode = aUBLInvoice.getDocumentCurrencyCodeValue ();
    try {
      aEbiInvoice.setInvoiceCurrency (Ebi40CurrencyType.fromValue (StringHelper.trim (sUBLCurrencyCode)));
    }
    catch (final IllegalArgumentException ex) {
      aTransformationErrorList.addError ("DocumentCurrencyCode",
                                         EText.INVALID_CURRENCY_CODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             sUBLCurrencyCode));
    }
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
                                           EText.BILLER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null) {
        // The customer's internal identifier for the supplier.
        aEbiBiller.setInvoiceRecipientsBillerID (aUBLSupplier.getCustomerAssignedAccountIDValue ());
      }
      if (StringHelper.hasNoText (aEbiBiller.getInvoiceRecipientsBillerID ())) {
        if (m_bStrictERBMode) {
          // Mandatory field
          aTransformationErrorList.addError ("AccountingSupplierParty/CustomerAssignedAccountID",
                                             EText.CUSTOMER_ASSIGNED_ACCOUNTID_MISSING.getDisplayText (m_aDisplayLocale));
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
                                           EText.SUPPLIER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
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
                                             EText.SUPPLIER_ASSIGNED_ACCOUNTID_MISSING.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                               DEFAULT_BILLERS_INVOICERECIPIENT_ID));
        aEbiRecipient.setBillersInvoiceRecipientID (DEFAULT_BILLERS_INVOICERECIPIENT_ID);
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
        aTransformationErrorList.addError ("OrderReference/ID",
                                           EText.ORDER_REFERENCE_MISSING.getDisplayText (m_aDisplayLocale));
      }
      else {
        if (sUBLOrderReferenceID != null && sUBLOrderReferenceID.length () > ORDER_REFERENCE_MAX_LENGTH) {
          aTransformationErrorList.addWarning ("OrderReference/ID",
                                               EText.ORDER_REFERENCE_TOO_LONG.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLOrderReferenceID,
                                                                                                      Integer.valueOf (ORDER_REFERENCE_MAX_LENGTH)));
          sUBLOrderReferenceID = sUBLOrderReferenceID.substring (0, ORDER_REFERENCE_MAX_LENGTH);
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
                                               EText.UNSUPPORTED_TAX_SCHEME.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                    sUBLTaxSchemeSchemeID,
                                                                                                    sUBLTaxSchemeID));
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
      int nInvoiceLineIndex = 0;
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
          aUBLPercent = BigDecimal.ZERO;
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/Item/ClassifiedTaxCategory",
                                               EText.DETAILS_TAX_PERCENTAGE_NOT_FOUND.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                              aUBLPercent));
        }

        // Start creating ebInterface line
        final Ebi40ListLineItemType aEbiListLineItem = new Ebi40ListLineItemType ();

        // Invoice line number
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (aUBLInvoiceLine.getIDValue ());
        if (aUBLPositionNumber == null) {
          aUBLPositionNumber = BigInteger.valueOf (nInvoiceLineIndex + 1);
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/ID",
                                               EText.DETAILS_INVALID_POSITION.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      aUBLInvoiceLine.getIDValue (),
                                                                                                      aUBLPositionNumber));
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
          // Unit code is optional
          if (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode () != null)
            aEbiQuantity.setUnit (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode ().value ());
          aEbiQuantity.setValue (aUBLInvoiceLine.getInvoicedQuantityValue ());
        }
        if (aEbiQuantity.getUnit () == null) {
          // ebInterface requires a quantity!
          aEbiQuantity.setUnit (EUnitOfMeasureCode20.C62.getID ());
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/InvoicedQuantity/UnitCode",
                                               EText.DETAILS_INVALID_UNIT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  aEbiQuantity.getUnit ()));
        }
        if (aEbiQuantity.getValue () == null) {
          aEbiQuantity.setValue (BigDecimal.ONE);
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/InvoicedQuantity",
                                               EText.DETAILS_INVALID_QUANTITY.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      aEbiQuantity.getValue ()));
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
      aTransformationErrorList.addError ("InvoiceLine", EText.VAT_ITEM_MISSING.getDisplayText (m_aDisplayLocale));
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

      int nAllowanceChargeIndex = 0;
      for (final AllowanceChargeType aUBLAllowanceCharge : aUBLInvoice.getAllowanceCharge ()) {
        final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();
        if (eSurcharge.isUndefined ())
          eSurcharge = ETriState.valueOf (bItemIsSurcharge);
        final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();

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
          aTransformationErrorList.addError ("Invoice/AllowanceCharge[" + nAllowanceChargeIndex + "]",
                                             EText.ALLOWANCE_CHARGE_NO_TAXRATE.getDisplayText (m_aDisplayLocale));
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
        ++nAllowanceChargeIndex;
      }
      aEbiInvoice.setReductionAndSurchargeDetails (aEbiRS);
    }

    // Total gross amount
    aEbiInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Payment method
    {
      int nPaymentMeansIndex = 0;
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
            aEbiAccount.setBIC (StringHelper.trim (aUBLFinancialAccount.getFinancialInstitutionBranch ()
                                                                       .getFinancialInstitution ()
                                                                       .getIDValue ()));
            if (!RegExHelper.stringMatchesPattern (REGEX_BIC, aEbiAccount.getBIC ())) {
              aTransformationErrorList.addError ("PaymentMeans[" +
                                                     nPaymentMeansIndex +
                                                     "]/PayeeFinancialAccount/FinancialInstitutionBranch/FinancialInstitution/ID",
                                                 EText.BIC_INVALID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                           aEbiAccount.getBIC ()));
              aEbiAccount.setBIC (null);
            }
          }

          // IBAN
          aEbiAccount.setIBAN (StringHelper.trim (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue ()));
          if (StringHelper.getLength (aEbiAccount.getIBAN ()) > IBAN_MAX_LENGTH) {
            aTransformationErrorList.addWarning ("PaymentMeans[" + nPaymentMeansIndex + "]/PayeeFinancialAccount/ID",
                                                 EText.IBAN_TOO_LONG.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             aEbiAccount.getIBAN (),
                                                                                             Integer.valueOf (IBAN_MAX_LENGTH)));
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
          aEbiAccount.setBankAccountOwner (sBankAccountOwnerName);

          aEbiUBTMethod.getBeneficiaryAccount ().add (aEbiAccount);
          aEbiInvoice.setPaymentMethod (aEbiUBTMethod);
          break;
        }
        ++nPaymentMeansIndex;
      }
    }

    // Delivery
    final Ebi40DeliveryType aEbiDelivery = new Ebi40DeliveryType ();
    {
      int nDeliveryIndex = 0;
      for (final DeliveryType aUBLDelivery : aUBLInvoice.getDelivery ()) {
        if (aUBLDelivery.getActualDeliveryDate () != null) {
          // Use the first delivery with a delivery date
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
              aTransformationErrorList.addError ("Delivery[" + nDeliveryIndex + "]/DeliveryParty",
                                                 EText.DELIVERY_WITHOUT_NAME.getDisplayText (m_aDisplayLocale));

            aEbiDelivery.setAddress (aEbiAddress);
          }
          break;
        }
        ++nDeliveryIndex;
      }

      if (aEbiDelivery.getDate () == null) {
        // No delivery date is present - check for service period
        final PeriodType aUBLInvoicePeriod = ContainerHelper.getSafe (aUBLInvoice.getInvoicePeriod (), 0);
        if (aUBLInvoicePeriod != null) {
          final Ebi40PeriodType aEbiPeriod = new Ebi40PeriodType ();
          aEbiPeriod.setFromDate (aUBLInvoicePeriod.getStartDateValue ());
          aEbiPeriod.setToDate (aUBLInvoicePeriod.getEndDateValue ());
          aEbiDelivery.setPeriod (aEbiPeriod);
        }
      }
    }

    if (m_bStrictERBMode) {
      if (aEbiDelivery.getDate () == null && aEbiDelivery.getPeriod () == null)
        aTransformationErrorList.addError ("Invoice", EText.NO_DELIVERY_DATE.getDisplayText (m_aDisplayLocale));
    }

    if (aEbiDelivery.getDate () != null || aEbiDelivery.getPeriod () != null)
      aEbiInvoice.setDelivery (aEbiDelivery);

    return aEbiInvoice;
  }
}
