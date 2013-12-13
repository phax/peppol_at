/**
 * Copyright (C) 2010-2013 Bundesrechenzentrum GmbH
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
package at.gv.brz.transform.ubl2ebi.creditnote;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.datatype.XMLGregorianCalendar;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PersonType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NoteType;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import at.gv.brz.transform.ubl2ebi.helper.SchemedID;
import at.gv.brz.transform.ubl2ebi.helper.TaxCategoryKey;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.locale.country.CountryCache;
import com.phloc.commons.math.MathHelper;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.state.ETriState;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.ebinterface.codelist.ETaxCode;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierType;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierTypeType;
import com.phloc.ebinterface.v40.Ebi40AddressType;
import com.phloc.ebinterface.v40.Ebi40BillerType;
import com.phloc.ebinterface.v40.Ebi40CountryCodeType;
import com.phloc.ebinterface.v40.Ebi40CountryType;
import com.phloc.ebinterface.v40.Ebi40CurrencyType;
import com.phloc.ebinterface.v40.Ebi40DeliveryType;
import com.phloc.ebinterface.v40.Ebi40DetailsType;
import com.phloc.ebinterface.v40.Ebi40DiscountType;
import com.phloc.ebinterface.v40.Ebi40DocumentTypeType;
import com.phloc.ebinterface.v40.Ebi40InvoiceRecipientType;
import com.phloc.ebinterface.v40.Ebi40InvoiceType;
import com.phloc.ebinterface.v40.Ebi40ItemListType;
import com.phloc.ebinterface.v40.Ebi40ItemType;
import com.phloc.ebinterface.v40.Ebi40ListLineItemType;
import com.phloc.ebinterface.v40.Ebi40NoPaymentType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceDetailType;
import com.phloc.ebinterface.v40.Ebi40OrderReferenceType;
import com.phloc.ebinterface.v40.Ebi40OtherTaxType;
import com.phloc.ebinterface.v40.Ebi40PaymentConditionsType;
import com.phloc.ebinterface.v40.Ebi40PeriodType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeBaseType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeDetailsType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeListLineItemDetailsType;
import com.phloc.ebinterface.v40.Ebi40ReductionAndSurchargeType;
import com.phloc.ebinterface.v40.Ebi40TaxRateType;
import com.phloc.ebinterface.v40.Ebi40TaxType;
import com.phloc.ebinterface.v40.Ebi40UnitType;
import com.phloc.ebinterface.v40.Ebi40VATType;
import com.phloc.ebinterface.v40.ObjectFactory;
import com.phloc.ubl21.codelist.EUnitOfMeasureCode21;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.peppol.codelist.ETaxSchemeID;

/**
 * Main converter between UBL 2.1 credit note and ebInterface 4.0 credit note.
 * 
 * @author philip
 */
@Immutable
public final class CreditNoteToEbInterface40Converter extends AbstractCreditNoteConverter
{
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
  public CreditNoteToEbInterface40Converter (@Nonnull final Locale aDisplayLocale,
                                             @Nonnull final Locale aContentLocale,
                                             final boolean bStrictERBMode)
  {
    super (aDisplayLocale, aContentLocale, bStrictERBMode);
  }

  private void _setAddressData (@Nullable final AddressType aUBLAddress,
                                @Nonnull final Ebi40AddressType aEbiAddress,
                                @Nonnull final String sPartyType,
                                @Nonnull final ErrorList aTransformationErrorList)
  {
    boolean bCountryErrorMsgEmitted = false;

    // Convert main address
    if (aUBLAddress != null)
    {
      aEbiAddress.setStreet (StringHelper.getImplodedNonEmpty (' ',
                                                               StringHelper.trim (aUBLAddress.getStreetNameValue ()),
                                                               StringHelper.trim (aUBLAddress.getBuildingNumberValue ())));
      aEbiAddress.setPOBox (StringHelper.trim (aUBLAddress.getPostboxValue ()));
      aEbiAddress.setTown (StringHelper.trim (aUBLAddress.getCityNameValue ()));
      aEbiAddress.setZIP (StringHelper.trim (aUBLAddress.getPostalZoneValue ()));

      // Country
      if (aUBLAddress.getCountry () != null)
      {
        final Ebi40CountryType aEbiCountry = new Ebi40CountryType ();
        final String sCountryCode = StringHelper.trim (aUBLAddress.getCountry ().getIdentificationCodeValue ());
        Ebi40CountryCodeType eEbiCountryCode = null;
        try
        {
          eEbiCountryCode = Ebi40CountryCodeType.fromValue (sCountryCode);
        }
        catch (final IllegalArgumentException ex)
        {
          aTransformationErrorList.addError (sPartyType + "/PostalAddress/Country/IdentificationCode",
                                             EText.ADDRESS_INVALID_COUNTRY.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                   sCountryCode));
          bCountryErrorMsgEmitted = true;
        }
        aEbiCountry.setCountryCode (eEbiCountryCode);

        final String sCountryName = StringHelper.trim (aUBLAddress.getCountry ().getNameValue ());
        aEbiCountry.setContent (sCountryName);
        if (StringHelper.hasNoText (sCountryName) && eEbiCountryCode != null)
        {
          // Write locale of country in content locale
          final Locale aLocale = CountryCache.getCountry (eEbiCountryCode.value ());
          if (aLocale != null)
            aEbiCountry.setContent (aLocale.getDisplayCountry (m_aContentLocale));
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
    if (aEbiAddress.getCountry () == null && !bCountryErrorMsgEmitted)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/Country/IdentificationCode",
                                         EText.ADDRESS_NO_COUNTRY.getDisplayText (m_aDisplayLocale));
  }

  @Nonnull
  private Ebi40AddressType _convertParty (@Nonnull final PartyType aUBLParty,
                                          @Nonnull final String sPartyType,
                                          @Nonnull final ErrorList aTransformationErrorList)
  {
    final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();

    if (aUBLParty.getPartyNameCount () > 1)
      aTransformationErrorList.addWarning (sPartyType + "/PartyName",
                                           EText.MULTIPLE_PARTIES.getDisplayText (m_aDisplayLocale));

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null)
      aEbiAddress.setName (StringHelper.trim (aUBLPartyName.getNameValue ()));

    if (aEbiAddress.getName () == null)
      aTransformationErrorList.addError (sPartyType, EText.PARTY_NO_NAME.getDisplayText (m_aDisplayLocale));

    // Convert main address
    _setAddressData (aUBLParty.getPostalAddress (), aEbiAddress, sPartyType, aTransformationErrorList);

    // Contact
    final ContactType aUBLContact = aUBLParty.getContact ();
    if (aUBLContact != null)
    {
      aEbiAddress.setPhone (StringHelper.trim (aUBLContact.getTelephoneValue ()));
      aEbiAddress.setEmail (StringHelper.trim (aUBLContact.getElectronicMailValue ()));
    }

    // Person name
    final List <String> ebContacts = new ArrayList <String> ();
    for (final PersonType aUBLPerson : aUBLParty.getPerson ())
    {
      ebContacts.add (StringHelper.getImplodedNonEmpty (' ',
                                                        StringHelper.trim (aUBLPerson.getTitleValue ()),
                                                        StringHelper.trim (aUBLPerson.getFirstNameValue ()),
                                                        StringHelper.trim (aUBLPerson.getMiddleNameValue ()),
                                                        StringHelper.trim (aUBLPerson.getFamilyNameValue ()),
                                                        StringHelper.trim (aUBLPerson.getNameSuffixValue ())));
    }
    if (!ebContacts.isEmpty ())
      aEbiAddress.setContact (StringHelper.getImplodedNonEmpty ('\n', ebContacts));

    // GLN and DUNS number
    if (aUBLParty.getEndpointID () != null)
    {
      final String sEndpointID = StringHelper.trim (aUBLParty.getEndpointIDValue ());
      if (StringHelper.hasText (sEndpointID))
      {
        // We have an endpoint ID

        // Check all identifier types
        final String sSchemeIDToSearch = StringHelper.trim (aUBLParty.getEndpointID ().getSchemeID ());

        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (sSchemeIDToSearch))
          {
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

    if (aEbiAddress.getAddressIdentifier () == null)
    {
      // check party identification
      int nPartyIdentificationIndex = 0;
      outer: for (final PartyIdentificationType aUBLPartyID : aUBLParty.getPartyIdentification ())
      {
        final String sUBLPartyID = StringHelper.trim (aUBLPartyID.getIDValue ());
        for (final Ebi40AddressIdentifierTypeType eType : Ebi40AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (aUBLPartyID.getID ().getSchemeID ()))
          {
            // Add GLN/DUNS number
            final Ebi40AddressIdentifierType aEbiType = new Ebi40AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setContent (sUBLPartyID);
            aEbiAddress.setAddressIdentifier (aEbiType);
            break outer;
          }
        aTransformationErrorList.addWarning (sPartyType + "/PartyIdentification[" + nPartyIdentificationIndex + "]",
                                             EText.PARTY_UNSUPPORTED_ADDRESS_IDENTIFIER.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                                sUBLPartyID,
                                                                                                                aUBLPartyID.getID ()
                                                                                                                           .getSchemeID ()));
        ++nPartyIdentificationIndex;
      }
    }

    return aEbiAddress;
  }

  public static boolean _isValidPaymentReferenceChecksum (@Nullable final String sChecksum)
  {
    if (StringHelper.getLength (sChecksum) == 1)
    {
      final char c = sChecksum.charAt (0);
      return (c >= '0' && c <= '9') || c == 'X';
    }
    return false;
  }

  @Nullable
  private String _makeAlphaNumType (@Nullable final String sText,
                                    @Nonnull final String sContext,
                                    @Nonnull final ErrorList aTransformationErrorList)
  {
    if (sText != null && !RegExHelper.stringMatchesPattern ("[0-9 | A-Z | a-z | -_äöüÄÖÜß]+", sText))
    {
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
   * Main conversion method to convert from UBL 2.0 to ebInterface 4.0
   * 
   * @param aUBLDoc
   *        The UBL invoice to be converted
   * @param aTransformationErrorList
   *        Error list. Must be empty!
   * @return The created ebInterface 4.0 document or <code>null</code> in case
   *         of a severe error.
   */
  @Nullable
  public Ebi40InvoiceType convertToEbInterface (@Nonnull final CreditNoteType aUBLDoc,
                                                @Nonnull final ErrorList aTransformationErrorList)
  {
    if (aUBLDoc == null)
      throw new NullPointerException ("UBLCreditNote");
    if (aTransformationErrorList == null)
      throw new NullPointerException ("TransformationErrorList");
    if (!aTransformationErrorList.isEmpty ())
      throw new IllegalArgumentException ("TransformationErrorList must be empty!");

    // Consistency check before starting the conversion
    _checkConsistency (aUBLDoc, aTransformationErrorList);
    if (aTransformationErrorList.containsAtLeastOneError ())
      return null;

    // Build ebInterface invoice
    final Ebi40InvoiceType aEbiDoc = new Ebi40InvoiceType ();
    aEbiDoc.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiDoc.setDocumentType (Ebi40DocumentTypeType.CREDIT_MEMO);

    // Cannot set the language, because the 3letter code is expected but we only
    // have the 2letter code!

    final String sUBLCurrencyCode = StringHelper.trim (aUBLDoc.getDocumentCurrencyCodeValue ());
    try
    {
      aEbiDoc.setInvoiceCurrency (Ebi40CurrencyType.fromValue (sUBLCurrencyCode));
    }
    catch (final IllegalArgumentException ex)
    {
      aTransformationErrorList.addError ("DocumentCurrencyCode",
                                         EText.INVALID_CURRENCY_CODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             sUBLCurrencyCode));
    }

    // CreditNote Number
    final String sCreditNoteNumber = StringHelper.trim (aUBLDoc.getIDValue ());
    if (StringHelper.hasNoText (sCreditNoteNumber))
      aTransformationErrorList.addError ("ID", EText.MISSING_INVOICE_NUMBER.getDisplayText (m_aDisplayLocale));
    else
      aEbiDoc.setInvoiceNumber (_makeAlphaNumType (sCreditNoteNumber, "ID", aTransformationErrorList));

    // Ignore the time!
    aEbiDoc.setInvoiceDate (aUBLDoc.getIssueDateValue ());
    if (aEbiDoc.getInvoiceDate () == null)
      aTransformationErrorList.addError ("IssueDate", EText.MISSING_INVOICE_DATE.getDisplayText (m_aDisplayLocale));

    // Biller/Supplier (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLDoc.getAccountingSupplierParty ();
      final Ebi40BillerType aEbiBiller = new Ebi40BillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiBiller.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiBiller.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingSupplierParty/Party/PartyTaxScheme",
                                           EText.BILLER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null)
      {
        // The customer's internal identifier for the supplier.
        aEbiBiller.setInvoiceRecipientsBillerID (StringHelper.trim (aUBLSupplier.getCustomerAssignedAccountIDValue ()));
      }
      if (StringHelper.hasNoText (aEbiBiller.getInvoiceRecipientsBillerID ()))
      {
        if (m_bStrictERBMode)
        {
          // Mandatory field
          aTransformationErrorList.addError ("AccountingSupplierParty/CustomerAssignedAccountID",
                                             EText.ERB_CUSTOMER_ASSIGNED_ACCOUNTID_MISSING.getDisplayText (m_aDisplayLocale));
        }
      }
      aEbiBiller.setAddress (_convertParty (aUBLSupplier.getParty (),
                                            "AccountingSupplierParty",
                                            aTransformationErrorList));
      aEbiDoc.setBiller (aEbiBiller);
    }

    // CreditNote recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLDoc.getAccountingCustomerParty ();
      final Ebi40InvoiceRecipientType aEbiRecipient = new Ebi40InvoiceRecipientType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLCustomer.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiRecipient.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiRecipient.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingCustomerParty/PartyTaxScheme",
                                           EText.SUPPLIER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null)
      {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        final String sBillersInvoiceRecipientID = StringHelper.trim (aUBLCustomer.getSupplierAssignedAccountIDValue ());
        aEbiRecipient.setBillersInvoiceRecipientID (sBillersInvoiceRecipientID);
      }
      if (StringHelper.hasNoText (aEbiRecipient.getBillersInvoiceRecipientID ()))
      {
        // Mandatory field
        aTransformationErrorList.addWarning ("AccountingCustomerParty/SupplierAssignedAccountID",
                                             EText.SUPPLIER_ASSIGNED_ACCOUNTID_MISSING.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                               DEFAULT_BILLERS_INVOICERECIPIENT_ID));
        aEbiRecipient.setBillersInvoiceRecipientID (DEFAULT_BILLERS_INVOICERECIPIENT_ID);
      }
      aEbiRecipient.setAddress (_convertParty (aUBLCustomer.getParty (),
                                               "AccountingCustomerParty",
                                               aTransformationErrorList));
      aEbiDoc.setInvoiceRecipient (aEbiRecipient);
    }

    // Order reference of invoice recipient
    String sUBLOrderReferenceID = null;
    {
      final OrderReferenceType aUBLOrderReference = aUBLDoc.getOrderReference ();
      if (aUBLOrderReference != null)
      {
        // Use directly from order reference
        sUBLOrderReferenceID = StringHelper.trim (aUBLOrderReference.getIDValue ());
      }
      if (StringHelper.hasNoText (sUBLOrderReferenceID))
      {
        // Check if a contract reference is present
        for (final DocumentReferenceType aDocumentReference : aUBLDoc.getContractDocumentReference ())
          if (StringHelper.hasTextAfterTrim (aDocumentReference.getIDValue ()))
          {
            sUBLOrderReferenceID = StringHelper.trim (aDocumentReference.getIDValue ());
            break;
          }
      }

      if (StringHelper.hasNoText (sUBLOrderReferenceID))
      {
        aTransformationErrorList.addError ("OrderReference/ID",
                                           EText.ORDER_REFERENCE_MISSING.getDisplayText (m_aDisplayLocale));
      }
      else
      {
        if (sUBLOrderReferenceID != null && sUBLOrderReferenceID.length () > ORDER_REFERENCE_MAX_LENGTH)
        {
          aTransformationErrorList.addWarning ("OrderReference/ID",
                                               EText.ORDER_REFERENCE_TOO_LONG.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLOrderReferenceID,
                                                                                                      Integer.valueOf (ORDER_REFERENCE_MAX_LENGTH)));
          sUBLOrderReferenceID = sUBLOrderReferenceID.substring (0, ORDER_REFERENCE_MAX_LENGTH);
        }

        sUBLOrderReferenceID = _makeAlphaNumType (sUBLOrderReferenceID, "OrderReference/ID", aTransformationErrorList);
      }

      final Ebi40OrderReferenceType aEbiOrderReference = new Ebi40OrderReferenceType ();
      aEbiOrderReference.setOrderID (sUBLOrderReferenceID);
      aEbiDoc.getInvoiceRecipient ().setOrderReference (aEbiOrderReference);
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final Ebi40TaxType aEbiTax = new Ebi40TaxType ();
    final Ebi40VATType aEbiVAT = new Ebi40VATType ();
    {
      int nTaxTotalIndex = 0;
      for (final TaxTotalType aUBLTaxTotal : aUBLDoc.getTaxTotal ())
      {
        int nTaxSubtotalIndex = 0;
        for (final TaxSubtotalType aUBLSubtotal : aUBLTaxTotal.getTaxSubtotal ())
        {
          // Tax category is a mandatory element
          final TaxCategoryType aUBLTaxCategory = aUBLSubtotal.getTaxCategory ();

          // Is the percentage value directly specified
          BigDecimal aUBLPercentage = aUBLTaxCategory.getPercentValue ();
          if (aUBLPercentage == null)
          {
            // no it is not :(
            final BigDecimal aUBLTaxAmount = aUBLSubtotal.getTaxAmountValue ();
            final BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmountValue ();
            if (aUBLTaxAmount != null && aUBLTaxableAmount != null)
            {
              // Calculate percentage
              aUBLPercentage = MathHelper.isEqualToZero (aUBLTaxableAmount) ? BigDecimal.ZERO
                                                                           : aUBLTaxAmount.multiply (CGlobal.BIGDEC_100)
                                                                                          .divide (aUBLTaxableAmount,
                                                                                                   SCALE_PERC,
                                                                                                   ROUNDING_MODE);
            }
          }

          BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmountValue ();
          if (aUBLTaxableAmount == null && aUBLPercentage != null)
          {
            // Calculate (inexact) subtotal
            aUBLTaxableAmount = MathHelper.isEqualToZero (aUBLPercentage) ? BigDecimal.ZERO
                                                                         : aUBLSubtotal.getTaxAmountValue ()
                                                                                       .multiply (CGlobal.BIGDEC_100)
                                                                                       .divide (aUBLPercentage,
                                                                                                SCALE_PRICE_LINE,
                                                                                                ROUNDING_MODE);
          }

          // Save item and put in map
          final String sUBLTaxSchemeSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ()
                                                                                 .getID ()
                                                                                 .getSchemeID ());
          final String sUBLTaxSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ().getIDValue ());

          final String sUBLTaxCategorySchemeID = StringHelper.trim (aUBLTaxCategory.getID ().getSchemeID ());
          final String sUBLTaxCategoryID = StringHelper.trim (aUBLTaxCategory.getID ().getValue ());

          aTaxCategoryPercMap.put (new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                       new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID)),
                                   aUBLPercentage);

          if (isSupportedTaxSchemeSchemeID (sUBLTaxSchemeSchemeID))
          {
            final ETaxSchemeID eUBLTaxScheme = ETaxSchemeID.getFromIDOrNull (sUBLTaxSchemeID);
            if (eUBLTaxScheme == null)
            {
              aTransformationErrorList.addError ("TaxTotal[" +
                                                     nTaxTotalIndex +
                                                     "]/TaxSubtotal[" +
                                                     nTaxSubtotalIndex +
                                                     "]/TaxCategory/TaxScheme/ID",
                                                 EText.UNSUPPORTED_TAX_SCHEME_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                         sUBLTaxSchemeID));
            }
            else
            {
              if (SUPPORTED_TAX_SCHEME_ID.equals (eUBLTaxScheme))
              {
                if (aUBLPercentage == null)
                {
                  aTransformationErrorList.addError ("TaxTotal[" +
                                                         nTaxTotalIndex +
                                                         "]/TaxSubtotal[" +
                                                         nTaxSubtotalIndex +
                                                         "]/TaxCategory/Percent",
                                                     EText.TAX_PERCENT_MISSING.getDisplayTextWithArgs (m_aDisplayLocale));
                }
                else
                {
                  // add VAT item
                  final Ebi40ItemType aEbiVATItem = new Ebi40ItemType ();
                  // Base amount
                  aEbiVATItem.setTaxedAmount (aUBLTaxableAmount);
                  // tax rate
                  final Ebi40TaxRateType aEbiVATTaxRate = new Ebi40TaxRateType ();
                  // Optional
                  if (false)
                    aEbiVATTaxRate.setTaxCode (sUBLTaxCategoryID);
                  aEbiVATTaxRate.setValue (aUBLPercentage);
                  aEbiVATItem.setTaxRate (aEbiVATTaxRate);
                  // Tax amount (mandatory)
                  aEbiVATItem.setAmount (aUBLSubtotal.getTaxAmountValue ());
                  // Add to list
                  aEbiVAT.getItem ().add (aEbiVATItem);
                }
              }
              else
              {
                // Other TAX
                final Ebi40OtherTaxType aOtherTax = new Ebi40OtherTaxType ();
                // As no comment is present, use the scheme ID
                aOtherTax.setComment (sUBLTaxSchemeID);
                // Tax amount (mandatory)
                aOtherTax.setAmount (aUBLSubtotal.getTaxAmountValue ());
                aEbiTax.getOtherTax ().add (aOtherTax);
              }
            }
          }
          else
          {
            aTransformationErrorList.addError ("TaxTotal[" +
                                                   nTaxTotalIndex +
                                                   "]/TaxSubtotal[" +
                                                   nTaxSubtotalIndex +
                                                   "]/TaxCategory/",
                                               EText.UNSUPPORTED_TAX_SCHEME.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                    sUBLTaxSchemeSchemeID,
                                                                                                    sUBLTaxSchemeID));
          }
          ++nTaxSubtotalIndex;
        }
        ++nTaxTotalIndex;
      }

      aEbiTax.setVAT (aEbiVAT);
      aEbiDoc.setTax (aEbiTax);
    }

    // Line items
    BigDecimal aTotalZeroPercLineExtensionAmount = BigDecimal.ZERO;
    {
      final Ebi40DetailsType aEbiDetails = new Ebi40DetailsType ();
      final Ebi40ItemListType aEbiItemList = new Ebi40ItemListType ();
      int nCreditNoteLineIndex = 0;
      for (final CreditNoteLineType aUBLCreditNoteLine : aUBLDoc.getCreditNoteLine ())
      {
        // Try to resolve tax category
        TaxCategoryType aUBLTaxCategory = ContainerHelper.getSafe (aUBLCreditNoteLine.getItem ()
                                                                                     .getClassifiedTaxCategory (), 0);
        if (aUBLTaxCategory == null)
        {
          // No direct tax category -> check if it is somewhere in the tax total
          outer: for (final TaxTotalType aUBLTaxTotal : aUBLCreditNoteLine.getTaxTotal ())
            for (final TaxSubtotalType aUBLTaxSubTotal : aUBLTaxTotal.getTaxSubtotal ())
            {
              // Only handle VAT items
              if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLTaxSubTotal.getTaxCategory ()
                                                                          .getTaxScheme ()
                                                                          .getIDValue ()))
              {
                // We found one -> just use it
                aUBLTaxCategory = aUBLTaxSubTotal.getTaxCategory ();
                break outer;
              }
            }
        }

        // Try to resolve tax percentage
        BigDecimal aUBLPercent = null;
        if (aUBLTaxCategory != null)
        {
          // Specified at tax category?
          if (aUBLTaxCategory.getPercent () != null)
            aUBLPercent = aUBLTaxCategory.getPercentValue ();

          if (aUBLPercent == null &&
              aUBLTaxCategory.getID () != null &&
              aUBLTaxCategory.getTaxScheme () != null &&
              aUBLTaxCategory.getTaxScheme ().getID () != null)
          {
            // Not specified - check from previous map
            final String sUBLTaxSchemeSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ()
                                                                                   .getID ()
                                                                                   .getSchemeID ());
            final String sUBLTaxSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ().getIDValue ());

            final String sUBLTaxCategorySchemeID = StringHelper.trim (aUBLTaxCategory.getID ().getSchemeID ());
            final String sUBLTaxCategoryID = StringHelper.trim (aUBLTaxCategory.getIDValue ());

            final TaxCategoryKey aKey = new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                            new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID));
            aUBLPercent = aTaxCategoryPercMap.get (aKey);
          }
        }
        if (aUBLPercent == null)
        {
          aUBLPercent = BigDecimal.ZERO;
          aTransformationErrorList.addWarning ("CreditNoteLine[" +
                                                   nCreditNoteLineIndex +
                                                   "]/Item/ClassifiedTaxCategory",
                                               EText.DETAILS_TAX_PERCENTAGE_NOT_FOUND.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                              aUBLPercent));
        }

        // Start creating ebInterface line
        final Ebi40ListLineItemType aEbiListLineItem = new Ebi40ListLineItemType ();

        // CreditNote line number
        final String sUBLPositionNumber = StringHelper.trim (aUBLCreditNoteLine.getIDValue ());
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (sUBLPositionNumber);
        if (aUBLPositionNumber == null)
        {
          aUBLPositionNumber = BigInteger.valueOf (nCreditNoteLineIndex + 1);
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nCreditNoteLineIndex + "]/ID",
                                               EText.DETAILS_INVALID_POSITION.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLPositionNumber,
                                                                                                      aUBLPositionNumber));
        }
        aEbiListLineItem.setPositionNumber (aUBLPositionNumber);

        // Descriptions
        for (final DescriptionType aUBLDescription : aUBLCreditNoteLine.getItem ().getDescription ())
          aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLDescription.getValue ()));
        if (aEbiListLineItem.getDescription ().isEmpty ())
        {
          // Use item name as description
          final NameType aUBLName = aUBLCreditNoteLine.getItem ().getName ();
          if (aUBLName != null)
            aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLName.getValue ()));
        }

        // Quantity
        final Ebi40UnitType aEbiQuantity = new Ebi40UnitType ();
        if (aUBLCreditNoteLine.getCreditedQuantity () != null)
        {
          // Unit code is optional
          if (aUBLCreditNoteLine.getCreditedQuantity ().getUnitCode () != null)
            aEbiQuantity.setUnit (StringHelper.trim (aUBLCreditNoteLine.getCreditedQuantity ().getUnitCode ()));
          aEbiQuantity.setValue (aUBLCreditNoteLine.getCreditedQuantityValue ());
        }
        if (aEbiQuantity.getUnit () == null)
        {
          // ebInterface requires a quantity!
          aEbiQuantity.setUnit (EUnitOfMeasureCode21.C62.getID ());
          aTransformationErrorList.addWarning ("CreditNoteLine[" +
                                                   nCreditNoteLineIndex +
                                                   "]/CreditNotedQuantity/UnitCode",
                                               EText.DETAILS_INVALID_UNIT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  aEbiQuantity.getUnit ()));
        }
        if (aEbiQuantity.getValue () == null)
        {
          aEbiQuantity.setValue (BigDecimal.ONE);
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nCreditNoteLineIndex + "]/CreditNotedQuantity",
                                               EText.DETAILS_INVALID_QUANTITY.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      aEbiQuantity.getValue ()));
        }
        aEbiListLineItem.setQuantity (aEbiQuantity);

        // Unit price
        if (aUBLCreditNoteLine.getPrice () != null)
        {
          // Unit price = priceAmount/baseQuantity (mandatory)
          final BigDecimal aUBLPriceAmount = aUBLCreditNoteLine.getPrice ().getPriceAmountValue ();
          // If no base quantity is present, assume 1 (optional)
          final BigDecimal aUBLBaseQuantity = aUBLCreditNoteLine.getPrice ().getBaseQuantityValue ();
          if (MathHelper.isEqualToZero (aEbiQuantity.getValue ()))
            aEbiListLineItem.setUnitPrice (BigDecimal.ZERO);
          else
            aEbiListLineItem.setUnitPrice (aUBLBaseQuantity == null ? aUBLPriceAmount
                                                                   : MathHelper.isEqualToZero (aUBLBaseQuantity) ? BigDecimal.ZERO
                                                                                                                : aUBLPriceAmount.divide (aUBLBaseQuantity,
                                                                                                                                          SCALE_PRICE_LINE,
                                                                                                                                          ROUNDING_MODE));
        }
        else
        {
          // Unit price = lineExtensionAmount / quantity (mandatory)
          final BigDecimal aUBLLineExtensionAmount = aUBLCreditNoteLine.getLineExtensionAmountValue ();
          if (MathHelper.isEqualToZero (aEbiQuantity.getValue ()))
            aEbiListLineItem.setUnitPrice (BigDecimal.ZERO);
          else
            aEbiListLineItem.setUnitPrice (aUBLLineExtensionAmount.divide (aEbiQuantity.getValue (),
                                                                           SCALE_PRICE_LINE,
                                                                           ROUNDING_MODE));
        }

        // Tax rate (mandatory)
        final Ebi40TaxRateType aEbiTaxRate = new Ebi40TaxRateType ();
        aEbiTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          // Optional
          if (false)
            aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aEbiListLineItem.setTaxRate (aEbiTaxRate);

        // Line item amount (quantity * unit price +- reduction / surcharge)
        aEbiListLineItem.setLineItemAmount (aUBLCreditNoteLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (MathHelper.isEqualToZero (aUBLPercent))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLCreditNoteLine.getLineExtensionAmountValue ());

        // Order reference per line (UBL 2.1 only)
        for (final OrderLineReferenceType aUBLOrderLineReference : aUBLCreditNoteLine.getOrderLineReference ())
          if (StringHelper.hasText (aUBLOrderLineReference.getLineIDValue ()))
          {
            final Ebi40OrderReferenceDetailType aEbiOrderRefDetail = new Ebi40OrderReferenceDetailType ();

            // order reference
            String sUBLLineOrderReferenceID = null;
            if (aUBLOrderLineReference.getOrderReference () != null)
              sUBLLineOrderReferenceID = StringHelper.trim (aUBLOrderLineReference.getOrderReference ().getIDValue ());
            if (StringHelper.hasNoText (sUBLLineOrderReferenceID))
            {
              // Use the global order reference from header level
              sUBLLineOrderReferenceID = sUBLOrderReferenceID;
            }
            aEbiOrderRefDetail.setOrderID (sUBLLineOrderReferenceID);

            // Order position number
            final String sOrderPosNumber = StringHelper.trim (aUBLOrderLineReference.getLineIDValue ());
            if (sOrderPosNumber != null)
            {
              if (sOrderPosNumber.length () == 0)
              {
                aTransformationErrorList.addError ("CreditNoteLine[" +
                                                       nCreditNoteLineIndex +
                                                       "]/OrderLineReference/LineID",
                                                   EText.ORDERLINE_REF_ID_EMPTY.getDisplayText (m_aDisplayLocale));
              }
              else
              {
                aEbiOrderRefDetail.setOrderPositionNumber (_makeAlphaNumType (sOrderPosNumber,
                                                                              "CreditNoteLine[" +
                                                                                  nCreditNoteLineIndex +
                                                                                  "]/OrderLineReference/LineID",
                                                                              aTransformationErrorList));
              }
            }
            aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderRefDetail);
            break;
          }

        // Reduction and surcharge (UBL 2.1 only)
        if (aUBLCreditNoteLine.hasAllowanceChargeEntries ())
        {
          // Start with quantity*unitPrice for base amount
          BigDecimal aEbiBaseAmount = aEbiListLineItem.getQuantity ()
                                                      .getValue ()
                                                      .multiply (aEbiListLineItem.getUnitPrice ());
          final Ebi40ReductionAndSurchargeListLineItemDetailsType aEbiRSDetails = new Ebi40ReductionAndSurchargeListLineItemDetailsType ();

          // ebInterface can handle only Reduction or only Surcharge
          ETriState eSurcharge = ETriState.UNDEFINED;
          for (final AllowanceChargeType aUBLAllowanceCharge : aUBLCreditNoteLine.getAllowanceCharge ())
          {
            final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();

            // Remember for next item
            if (eSurcharge.isUndefined ())
              eSurcharge = ETriState.valueOf (bItemIsSurcharge);
            final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();

            final Ebi40ReductionAndSurchargeBaseType aEbiRSItem = new Ebi40ReductionAndSurchargeBaseType ();
            // Amount is mandatory
            final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
            aEbiRSItem.setAmount (bSwapSigns ? aAmount.negate () : aAmount);

            // Base amount is optional
            if (aUBLAllowanceCharge.getBaseAmount () != null)
              aEbiRSItem.setBaseAmount (aUBLAllowanceCharge.getBaseAmountValue ());
            if (aEbiRSItem.getBaseAmount () == null)
              aEbiRSItem.setBaseAmount (aEbiBaseAmount);

            if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null)
            {
              // Percentage is optional
              final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ()
                                                          .multiply (CGlobal.BIGDEC_100);
              aEbiRSItem.setPercentage (bSwapSigns ? aPerc.negate () : aPerc);
            }

            if (eSurcharge.isTrue ())
            {
              aEbiRSDetails.getSurchargeListLineItem ().add (aEbiRSItem);
              aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
            }
            else
            {
              aEbiRSDetails.getReductionListLineItem ().add (aEbiRSItem);
              aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
            }
          }
          aEbiListLineItem.setReductionAndSurchargeListLineItemDetails (aEbiRSDetails);
        }

        // Add the item to the list
        aEbiItemList.getListLineItem ().add (aEbiListLineItem);
        nCreditNoteLineIndex++;
      }
      aEbiDetails.getItemList ().add (aEbiItemList);
      aEbiDoc.setDetails (aEbiDetails);
    }

    if (aEbiVAT.hasNoItemEntries ())
    {
      aTransformationErrorList.addError ("CreditNoteLine", EText.VAT_ITEM_MISSING.getDisplayText (m_aDisplayLocale));
      if (false)
      {
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
    if (aUBLDoc.hasAllowanceChargeEntries ())
    {
      // Start with quantity*unitPrice for base amount
      BigDecimal aEbiBaseAmount = aUBLDoc.getLegalMonetaryTotal ().getLineExtensionAmountValue ();
      final Ebi40ReductionAndSurchargeDetailsType aEbiRS = new Ebi40ReductionAndSurchargeDetailsType ();

      // ebInterface can handle only Reduction or only Surcharge
      ETriState eSurcharge = ETriState.UNDEFINED;

      int nAllowanceChargeIndex = 0;
      for (final AllowanceChargeType aUBLAllowanceCharge : aUBLDoc.getAllowanceCharge ())
      {
        final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();
        if (eSurcharge.isUndefined ())
          eSurcharge = ETriState.valueOf (bItemIsSurcharge);
        final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();

        final Ebi40ReductionAndSurchargeType aEbiRSItem = new Ebi40ReductionAndSurchargeType ();
        // Amount is mandatory
        final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
        aEbiRSItem.setAmount (bSwapSigns ? aAmount.negate () : aAmount);

        // Base amount is optional
        if (aUBLAllowanceCharge.getBaseAmount () != null)
          aEbiRSItem.setBaseAmount (aUBLAllowanceCharge.getBaseAmountValue ());
        if (aEbiRSItem.getBaseAmount () == null)
          aEbiRSItem.setBaseAmount (aEbiBaseAmount);

        if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null)
        {
          // Percentage is optional
          final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ().multiply (CGlobal.BIGDEC_100);
          aEbiRSItem.setPercentage (bSwapSigns ? aPerc.negate () : aPerc);
        }

        Ebi40TaxRateType aEbiTaxRate = null;
        for (final TaxCategoryType aUBLTaxCategory : aUBLAllowanceCharge.getTaxCategory ())
          if (aUBLTaxCategory.getPercent () != null)
          {
            aEbiTaxRate = new Ebi40TaxRateType ();
            aEbiTaxRate.setValue (aUBLTaxCategory.getPercentValue ());
            if (false)
              aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
            break;
          }
        if (aEbiTaxRate == null)
        {
          aTransformationErrorList.addError ("CreditNote/AllowanceCharge[" + nAllowanceChargeIndex + "]",
                                             EText.ALLOWANCE_CHARGE_NO_TAXRATE.getDisplayText (m_aDisplayLocale));
          // No default in this case
          if (false)
          {
            aEbiTaxRate = new Ebi40TaxRateType ();
            aEbiTaxRate.setValue (BigDecimal.ZERO);
            aEbiTaxRate.setTaxCode (ETaxCode.NOT_TAXABLE.getID ());
          }
        }
        aEbiRSItem.setTaxRate (aEbiTaxRate);

        if (eSurcharge.isTrue ())
        {
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createSurcharge (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
        }
        else
        {
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createReduction (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
        }
        aEbiDoc.setReductionAndSurchargeDetails (aEbiRS);
        ++nAllowanceChargeIndex;
      }
    }

    // PrepaidAmount is not supported!
    if (aUBLDoc.getLegalMonetaryTotal ().getPrepaidAmount () != null &&
        !MathHelper.isEqualToZero (aUBLDoc.getLegalMonetaryTotal ().getPrepaidAmountValue ()))
    {
      aTransformationErrorList.addError ("CreditNote/LegalMonetaryTotal/PrepaidAmount",
                                         EText.PREPAID_NOT_SUPPORTED.getDisplayText (m_aDisplayLocale));
    }

    // Total gross amount
    aEbiDoc.setTotalGrossAmount (aUBLDoc.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Always no payment
    final Ebi40PaymentConditionsType aEbiPaymentConditions = new Ebi40PaymentConditionsType ();
    final Ebi40NoPaymentType aEbiNoPayment = new Ebi40NoPaymentType ();
    aEbiDoc.setPaymentMethod (aEbiNoPayment);

    if (m_bStrictERBMode)
    {
      if (aEbiDoc.getPaymentMethod () == null)
        aTransformationErrorList.addError ("CreditNote", EText.ERB_NO_PAYMENT_METHOD.getDisplayText (m_aDisplayLocale));
    }

    // Payment terms (UBL 2.1 only)
    {
      final List <String> aPaymentConditionsNotes = new ArrayList <String> ();
      int nPaymentTermsIndex = 0;
      for (final PaymentTermsType aUBLPaymentTerms : aUBLDoc.getPaymentTerms ())
      {
        if (aUBLPaymentTerms.getSettlementDiscountPercent () != null)
        {
          if (aUBLPaymentTerms.getSettlementPeriod () == null ||
              aUBLPaymentTerms.getSettlementPeriod ().getEndDate () == null)
          {
            aTransformationErrorList.addWarning ("PaymentTerms[" + nPaymentTermsIndex + "]/SettlementPeriod",
                                                 EText.SETTLEMENT_PERIOD_MISSING.getDisplayText (m_aDisplayLocale));
          }
          else
          {
            // Add notes
            for (final NoteType aUBLNote : aUBLPaymentTerms.getNote ())
            {
              final String sUBLNote = StringHelper.trim (aUBLNote.getValue ());
              if (StringHelper.hasText (sUBLNote))
                aPaymentConditionsNotes.add (sUBLNote);
            }

            final Ebi40DiscountType aEbiDiscount = new Ebi40DiscountType ();
            aEbiDiscount.setPaymentDate (aUBLPaymentTerms.getSettlementPeriod ().getEndDateValue ());
            aEbiDiscount.setPercentage (aUBLPaymentTerms.getSettlementDiscountPercentValue ());
            // Optional amount value
            aEbiDiscount.setAmount (aUBLPaymentTerms.getAmountValue ());
            aEbiPaymentConditions.getDiscount ().add (aEbiDiscount);
          }
        }
        else
        {
          aTransformationErrorList.addWarning ("PaymentTerms[" + nPaymentTermsIndex + "]",
                                               EText.PENALTY_NOT_ALLOWED.getDisplayText (m_aDisplayLocale));
        }

        ++nPaymentTermsIndex;
      }

      if (!aPaymentConditionsNotes.isEmpty ())
        aEbiPaymentConditions.setComment (StringHelper.getImploded ('\n', aPaymentConditionsNotes));
    }

    if (aEbiPaymentConditions.getDueDate () == null)
    {
      // ebInterface requires due date
      if (aEbiPaymentConditions.hasDiscountEntries ())
        aTransformationErrorList.addError ("PaymentMeans/PaymentDueDate",
                                           EText.DISCOUNT_WITHOUT_DUEDATE.getDisplayTextWithArgs (m_aDisplayLocale));
    }
    else
    {
      // Independent if discounts are present or not
      aEbiDoc.setPaymentConditions (aEbiPaymentConditions);
    }

    // Delivery
    final Ebi40DeliveryType aEbiDelivery = new Ebi40DeliveryType ();
    {
      int nDeliveryIndex = 0;
      for (final DeliveryType aUBLDelivery : aUBLDoc.getDelivery ())
      {
        if (aUBLDelivery.getActualDeliveryDate () != null)
        {
          // Use the first delivery with a delivery date
          aEbiDelivery.setDate (aUBLDelivery.getActualDeliveryDateValue ());

          // Address
          if (aUBLDelivery.getDeliveryLocation () != null && aUBLDelivery.getDeliveryLocation ().getAddress () != null)
          {
            final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();
            _setAddressData (aUBLDelivery.getDeliveryLocation ().getAddress (),
                             aEbiAddress,
                             "Delivery",
                             aTransformationErrorList);

            // Check delivery party
            String sAddressName = null;
            if (aUBLDelivery.getDeliveryParty () != null)
              for (final PartyNameType aUBLPartyName : aUBLDelivery.getDeliveryParty ().getPartyName ())
              {
                sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                if (StringHelper.hasText (sAddressName))
                  break;
              }

            // As fallback use accounting customer party
            if (StringHelper.hasNoText (sAddressName) &&
                aUBLDoc.getAccountingCustomerParty () != null &&
                aUBLDoc.getAccountingCustomerParty ().getParty () != null)
            {
              for (final PartyNameType aUBLPartyName : aUBLDoc.getAccountingCustomerParty ()
                                                              .getParty ()
                                                              .getPartyName ())
              {
                sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                if (StringHelper.hasText (sAddressName))
                  break;
              }
            }
            aEbiAddress.setName (sAddressName);

            if (StringHelper.hasNoText (aEbiAddress.getName ()))
              aTransformationErrorList.addError ("Delivery[" + nDeliveryIndex + "]/DeliveryParty",
                                                 EText.DELIVERY_WITHOUT_NAME.getDisplayText (m_aDisplayLocale));

            aEbiDelivery.setAddress (aEbiAddress);
          }
          break;
        }
        ++nDeliveryIndex;
      }
      if (aEbiDelivery.getDate () == null)
      {
        // No delivery date is present - check for service period
        final PeriodType aUBLCreditNotePeriod = ContainerHelper.getSafe (aUBLDoc.getInvoicePeriod (), 0);
        if (aUBLCreditNotePeriod != null)
        {
          final XMLGregorianCalendar aStartDate = aUBLCreditNotePeriod.getStartDateValue ();
          final XMLGregorianCalendar aEndDate = aUBLCreditNotePeriod.getEndDateValue ();
          if (aStartDate != null)
          {
            if (aEndDate == null)
            {
              // It's just a date
              aEbiDelivery.setDate (aStartDate);
            }
            else
            {
              // It's a period!
              final Ebi40PeriodType aEbiPeriod = new Ebi40PeriodType ();
              aEbiPeriod.setFromDate (aStartDate);
              aEbiPeriod.setToDate (aEndDate);
              aEbiDelivery.setPeriod (aEbiPeriod);
            }
          }
        }
      }
    }

    if (m_bStrictERBMode)
    {
      if (aEbiDelivery.getDate () == null && aEbiDelivery.getPeriod () == null)
        aTransformationErrorList.addError ("CreditNote", EText.ERB_NO_DELIVERY_DATE.getDisplayText (m_aDisplayLocale));
    }

    if (aEbiDelivery.getDate () != null || aEbiDelivery.getPeriod () != null)
      aEbiDoc.setDelivery (aEbiDelivery);

    return aEbiDoc;
  }
}
