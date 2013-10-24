/**
 * Copyright (C) 2010 Bundesrechenzentrum GmbH
 * http://www.brz.gv.at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.gv.brz.transform.ubl2ebi;

import java.math.RoundingMode;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoiceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ProfileIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionIDType;
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;

import com.phloc.commons.annotations.Translatable;
import com.phloc.commons.name.IHasDisplayText;
import com.phloc.commons.name.IHasDisplayTextWithArgs;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.text.ITextProvider;
import com.phloc.commons.text.impl.TextProvider;
import com.phloc.commons.text.resolve.DefaultTextResolver;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.peppol.codelist.ETaxSchemeID;
import eu.europa.ec.cipa.peppol.identifier.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.IPeppolPredefinedProcessIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.PredefinedProcessIdentifierManager;

/**
 * Base class for PEPPOL 2.0 to ebInterface converter
 * 
 * @author philip
 */
@Immutable
public abstract class AbstractPEPPOLUBL20ToEbInterfaceConverter
{
  public static final String DEFAULT_BILLERS_INVOICERECIPIENT_ID = "000";

  @Translatable
  public static enum EText implements IHasDisplayText, IHasDisplayTextWithArgs
  {
    NO_UBL_VERSION_ID ("Die UBLVersionID fehlt. Es wird der Wert ''{0}'' erwartet.", "No UBLVersionID present. It must be ''{0}''."),
    INVALID_UBL_VERSION_ID ("Die UBLVersionID ''{0}'' ist ungültig. Diese muss den Wert ''{1}'' haben.", "Invalid UBLVersionID value ''{0}'' present. It must be ''{1}''."),
    NO_PROFILE_ID ("Die ProfileID fehlt", "No ProfileID present."),
    INVALID_PROFILE_ID ("Die ProfileID ''{0}'' ist ungültig.", "Invalid ProfileID value ''{0}'' present."),
    NO_CUSTOMIZATION_ID ("Die CustomizationID fehlt", "No CustomizationID present."),
    INVALID_CUSTOMIZATION_SCHEME_ID ("Die CustomizationID schemeID ''{0}'' ist ungültig. Diese muss den Wert ''{1}'' haben.", "Invalid CustomizationID schemeID value ''{0}'' present. It must be ''{1}''."),
    INVALID_CUSTOMIZATION_ID ("Die angegebene CustomizationID ''{0}'' ist ungültig. Sie wird vom angegebenen Profil nicht unterstützt.", "Invalid CustomizationID value ''{0}'' present. It is not supported by the passed profile."),
    NO_INVOICE_TYPECODE ("Der InvoiceTypeCode fehlt. Es wird der Wert ''{0}'' erwartet.", "No InvoiceTypeCode present. It must be ''{0}''."),
    INVALID_INVOICE_TYPECODE ("Der InvoiceTypeCode ''{0}'' ist ungültig. Dieser muss den Wert ''{1}'' haben.", "Invalid InvoiceTypeCode value ''{0}'' present. It must be ''{1}''."),
    ADDRESS_NO_STREET ("In der Adresse fehlt die Straße.", "Address is missing a street name."),
    ADDRESS_NO_CITY ("In der Adresse fehlt der Name der Stadt.", "Address is missing a city name."),
    ADDRESS_NO_ZIPCODE ("In der Adresse fehlt die PLZ.", "Address is missing a ZIP code."),
    ADDRESS_INVALID_COUNTRY ("Der angegebene Ländercode ''{0}'' ist ungültig.", "The provided country code ''{0}'' is invalid."),
    ADDRESS_NO_COUNTRY ("In der Adresse fehlt der Name des Landes.", "Address is missing a country."),
    MULTIPLE_PARTIES ("Es sind mehrere Partynamen vorhanden - nur der erste wird verwendet.", "Multiple party names present - only the first one is used."),
    PARTY_NO_NAME ("Der Name der Party fehlt.", "Party name is missing."),
    PARTY_UNSUPPORTED_ENDPOINT ("Ignoriere den Enpunkt ''{0}'' des Typs ''{1}''.", "Ignoring endpoint ID ''{0}'' of type ''{1}''."),
    PARTY_UNSUPPORTED_ADDRESS_IDENTIFIER ("Ignoriere die ID ''{0}'' des Typs ''{1}''.", "Ignoring identification ''{0}'' of type ''{1}''."),
    ALPHANUM_ID_TYPE_CHANGE ("''{0}'' ist ein ungültiger Typ und wurde auf ''{1}'' geändert.", "''{0}'' is an invalid value and was changed to ''{1}''."),
    INVALID_CURRENCY_CODE ("Der angegebene Währungscode ''{0}'' ist ungültig.", "Invalid currency code ''{0}'' provided."),
    MISSING_INVOICE_NUMBER ("Es wurde keine Rechnungsnummer angegeben.", "No invoice number was provided."),
    MISSING_INVOICE_DATE ("Es wurde keine Rechnungsdatum angegeben.", "No invoice date was provided."),
    BILLER_VAT_MISSING ("Die UID-Nummer des Rechnungsstellers fehlt. Verwenden Sie 'ATU00000000' für österreichische Rechnungssteller an wenn keine UID-Nummer notwendig ist.", "Failed to get biller VAT identification number. Use 'ATU00000000' for Austrian invoice recipients if no VAT identification number is required."),
    ERB_CUSTOMER_ASSIGNED_ACCOUNTID_MISSING ("Die ID des Rechnungsstellers beim Rechnungsempfänger fehlt.", "Failed to get customer assigned account ID for supplier."),
    SUPPLIER_VAT_MISSING ("Die UID-Nummer des Rechnungsempfängers fehlt. Verwenden Sie 'ATU00000000' für österreichische Empfänger an wenn keine UID-Nummer notwendig ist.", "Failed to get supplier VAT identification number. Use 'ATU00000000' for Austrian invoice recipients if no VAT identification number is required."),
    SUPPLIER_ASSIGNED_ACCOUNTID_MISSING ("Die ID des Rechnungsempfängers beim Rechnungssteller fehlt. Der Standardwert ''{0}'' wird verwendet.", "Failed to get supplier assigned account ID for customer. Defaulting to ''{0}''."),
    ORDER_REFERENCE_MISSING ("Die Auftragsreferenz fehlt.", "Failed to get order reference ID."),
    ORDER_REFERENCE_TOO_LONG ("Die Auftragsreferenz ''{0}'' ist zu lang und wurde nach {1} Zeichen abgeschnitten.", "Order reference value ''{0}'' is too long and was cut to {1} characters."),
    UNSUPPORTED_TAX_SCHEME_ID ("Die Steuerschema ID ''{0}'' ist ungültig.", "The tax scheme ID ''{0}'' is invalid."),
    TAX_PERCENT_MISSING ("Es konnte kein Steuersatz für diese Steuerkategorie ermittelt werden.", "No tax percentage could be determined for this tax category."),
    UNSUPPORTED_TAX_SCHEME ("Nicht unterstütztes Steuerschema gefunden: ''{0}'' und ''{1}''.", "Other tax scheme found and ignored: ''{0}'' and ''{1}''."),
    DETAILS_TAX_PERCENTAGE_NOT_FOUND ("Der Steuersatz der Rechnungszeile konnte nicht ermittelt werden. Verwende den Standardwert {0}%.", "Failed to resolve tax percentage for invoice line. Defaulting to {0}%."),
    DETAILS_INVALID_POSITION ("Die Rechnungspositionsnummer ''{0}'' ist nicht numerisch. Es wird der Index {1} verwendet.", "The UBL invoice line ID ''{0}'' is not numeric. Defaulting to index {1}."),
    DETAILS_INVALID_UNIT ("Die Rechnungszeile hat keine Mengeneinheit. Verwende den Standardwert ''{0}''.", "The UBL invoice line has no unit of measure. Defaulting to ''{0}''."),
    DETAILS_INVALID_QUANTITY ("Die Rechnungszeile hat keine Menge. Verwende den Standardwert ''{0}''.", "The UBL invoice line has no quantity. Defaulting to ''{0}''."),
    VAT_ITEM_MISSING ("Keine einzige Steuersumme gefunden", "No single VAT item found."),
    ALLOWANCE_CHARGE_NO_TAXRATE ("Die Steuerprozentrate für den globalen Zuschlag/Abschlag konnte nicht ermittelt werden.", "Failed to resolve tax rate percentage for global AllowanceCharge."),
    PAYMENTMEANS_CODE_INVALID ("Der PaymentMeansCode ist ungültig. Für Überweisungen muss {0} verwenden werden und für Lastschriftverfahren {1}.", "The PaymentMeansCode is invalid. For debit transfer use {0} and for direct debit use {1}."),
    PAYMENT_ID_NOT_NUMERIC ("Die Zahlungsreferenz ''{0}'' ist nicht numerisch und wird ignoriert.", "The payment reference ''{0}'' is not numeric and therefore ignored."),
    PAYMENT_ID_CHECKSUM_INVALID ("Die Checksumme ''{0}'' der Zahlungsreferenz ist ungültig wird ignoriert.", "The payment reference checksum ''{0}'' is invalid and therefore ignored."),
    PAYMENT_ID_TOO_LONG ("Die Zahlungsreferenz ''{0}'' ist zu lang und wird ignoriert.", "The payment reference ''{0}'' is too long and therefore ignored."),
    BIC_INVALID ("Der BIC ''{0}'' ist ungültig.", "The BIC ''{0}'' is invalid."),
    IBAN_TOO_LONG ("Der IBAN ''{0}'' ist zu lang. Er wurde nach {1} Zeichen abgeschnitten.", "The IBAN ''{0}'' is too long and was cut to {1} characters."),
    PAYMENTMEANS_UNSUPPORTED_CHANNELCODE ("Die Zahlungsart with dem ChannelCode ''{0}'' wird ignoriert.", "The payment means with ChannelCode ''{0}'' are ignored."),
    ERB_NO_PAYMENT_METHOD ("Es muss eine Zahlungsart angegeben werden.", "A payment method must be provided."),
    SETTLEMENT_PERIOD_MISSING ("Für Skontoeinträge muss mindestens ein Endedatum angegeben werden.", "Discount items require a settlement end date."),
    PENALTY_NOT_ALLOWED ("Strafzuschläge werden in ebInterface nicht unterstützt.", "Penalty surcharges are not supported in ebInterface."),
    DISCOUNT_WITHOUT_DUEDATE ("Skontoeinträge können nur angegeben werden, wenn auch ein Zahlungsziel angegeben wurde.", "Discount items can only be provided if a payment due date is present."),
    DELIVERY_WITHOUT_NAME ("Wenn eine Delivery/DeliveryLocation/Address angegeben ist muss auch ein Delivery/DeliveryParty/PartyName/Name angegeben werden.", "If a Delivery/DeliveryLocation/Address is present, a Delivery/DeliveryParty/PartyName/Name must also be present."),
    ERB_NO_DELIVERY_DATE ("Ein Lieferdatum oder ein Leistungszeitraum muss vorhanden sein.", "A Delivery/DeliveryDate or an InvoicePeriod must be present.");

    private final ITextProvider m_aTP;

    private EText (@Nonnull final String sDE, @Nonnull final String sEN)
    {
      m_aTP = TextProvider.create_DE_EN (sDE, sEN);
    }

    @Nullable
    public String getDisplayText (@Nonnull final Locale aContentLocale)
    {
      return DefaultTextResolver.getText (this, m_aTP, aContentLocale);
    }

    @Nullable
    public String getDisplayTextWithArgs (@Nonnull final Locale aContentLocale, @Nullable final Object... aArgs)
    {
      return DefaultTextResolver.getTextWithArgs (this, m_aTP, aContentLocale, aArgs);
    }
  }

  public static final int ORDER_REFERENCE_MAX_LENGTH = 34;
  public static final String REGEX_BIC = "[0-9A-Za-z]{8}([0-9A-Za-z]{3})?";
  public static final String SUPPORTED_TAX_SCHEME_SCHEME_ID = "UN/ECE 5153";
  public static final String SUPPORTED_TAX_SCHEME_SCHEME_ID_SUBSET = SUPPORTED_TAX_SCHEME_SCHEME_ID + " Subset";
  public static final int IBAN_MAX_LENGTH = 34;
  public static final String PAYMENT_CHANNEL_CODE_IBAN = "IBAN";
  public static final ETaxSchemeID SUPPORTED_TAX_SCHEME_ID = ETaxSchemeID.VALUE_ADDED_TAX;
  public static final String EBI_GENERATING_SYSTEM = "UBL 2.0 to ebInterface 4.0 converter";
  public static final int SCALE_PERC = 2;
  public static final int SCALE_PRICE_LINE = 4;
  // Austria uses HALF_UP mode!
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  protected final Locale m_aDisplayLocale;
  protected final Locale m_aContentLocale;
  protected final boolean m_bStrictERBMode;

  /**
   * Constructor
   * 
   * @param aDisplayLocale
   *        The locale for error messages. May not be <code>null</code>.
   * @param aContentLocale
   *        The locale for the created ebInterface files. May not be
   *        <code>null</code>.
   * @param bStrictERBMode
   *        <code>true</code> if ER>B specific checks should be performed
   */
  public AbstractPEPPOLUBL20ToEbInterfaceConverter (@Nonnull final Locale aDisplayLocale,
                                                    @Nonnull final Locale aContentLocale,
                                                    final boolean bStrictERBMode)
  {
    if (aDisplayLocale == null)
      throw new NullPointerException ("DisplayLocale");
    if (aContentLocale == null)
      throw new NullPointerException ("ContentLocale");

    m_aDisplayLocale = aDisplayLocale;
    m_aContentLocale = aContentLocale;
    m_bStrictERBMode = bStrictERBMode;
  }

  /**
   * Check if the passed UBL invoice is transformable
   * 
   * @param aUBLInvoice
   *        The UBL invoice to check
   */
  protected final void _checkConsistency (@Nonnull final InvoiceType aUBLInvoice,
                                          @Nonnull final ErrorList aTransformationErrorList)
  {
    // Check UBLVersionID
    final UBLVersionIDType aUBLVersionID = aUBLInvoice.getUBLVersionID ();
    if (aUBLVersionID == null)
      aTransformationErrorList.addError ("UBLVersionID",
                                         EText.NO_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                         CPeppolUBL.UBL_VERSION));
    else
    {
      final String sUBLVersionID = StringHelper.trim (aUBLVersionID.getValue ());
      if (!CPeppolUBL.UBL_VERSION.equals (sUBLVersionID))
        aTransformationErrorList.addError ("UBLVersionID",
                                           EText.INVALID_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                sUBLVersionID,
                                                                                                CPeppolUBL.UBL_VERSION));
    }

    // Check ProfileID
    IPeppolPredefinedProcessIdentifier aProcID = null;
    final ProfileIDType aProfileID = aUBLInvoice.getProfileID ();
    if (aProfileID == null)
      aTransformationErrorList.addError ("ProfileID", EText.NO_PROFILE_ID.getDisplayText (m_aDisplayLocale));
    else
    {
      final String sProfileID = StringHelper.trim (aProfileID.getValue ());
      aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
      if (aProcID == null)
        aTransformationErrorList.addError ("ProfileID",
                                           EText.INVALID_PROFILE_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                            sProfileID));
    }

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false)
    {
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
          if (aProcID != null)
          {
            final String sCustomizationID = StringHelper.trim (aCustomizationID.getValue ());
            IPeppolPredefinedDocumentTypeIdentifier aMatchingDocID = null;
            for (final IPeppolPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
              if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID))
              {
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
    if (aInvoiceTypeCode == null)
    {
      // None present
      aTransformationErrorList.addWarning ("InvoiceTypeCode",
                                           EText.NO_INVOICE_TYPECODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             CPeppolUBL.INVOICE_TYPE_CODE));
    }
    else
    {
      // If one is present, it must match
      final String sInvoiceTypeCode = StringHelper.trim (aInvoiceTypeCode.getValue ());
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (sInvoiceTypeCode))
        aTransformationErrorList.addError ("InvoiceTypeCode",
                                           EText.INVALID_INVOICE_TYPECODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  sInvoiceTypeCode,
                                                                                                  CPeppolUBL.INVOICE_TYPE_CODE));
    }
  }

  protected static boolean _isSupportedTaxSchemeSchemeID (@Nullable final String sUBLTaxSchemeSchemeID)
  {
    return sUBLTaxSchemeSchemeID == null ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID) ||
           sUBLTaxSchemeSchemeID.equals (SUPPORTED_TAX_SCHEME_SCHEME_ID_SUBSET);
  }
}
