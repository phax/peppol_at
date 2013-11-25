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
package at.gv.brz.transform.ubl2ebi.invoice;

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
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PersonType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InstructionNoteType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NoteType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentIDType;
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;
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
import com.phloc.ebinterface.v41.Ebi41AccountType;
import com.phloc.ebinterface.v41.Ebi41AddressIdentifierType;
import com.phloc.ebinterface.v41.Ebi41AddressIdentifierTypeType;
import com.phloc.ebinterface.v41.Ebi41AddressType;
import com.phloc.ebinterface.v41.Ebi41BillerType;
import com.phloc.ebinterface.v41.Ebi41CountryCodeType;
import com.phloc.ebinterface.v41.Ebi41CountryType;
import com.phloc.ebinterface.v41.Ebi41CurrencyType;
import com.phloc.ebinterface.v41.Ebi41DeliveryType;
import com.phloc.ebinterface.v41.Ebi41DetailsType;
import com.phloc.ebinterface.v41.Ebi41DirectDebitType;
import com.phloc.ebinterface.v41.Ebi41DiscountType;
import com.phloc.ebinterface.v41.Ebi41DocumentTypeType;
import com.phloc.ebinterface.v41.Ebi41FurtherIdentificationType;
import com.phloc.ebinterface.v41.Ebi41InvoiceRecipientType;
import com.phloc.ebinterface.v41.Ebi41InvoiceType;
import com.phloc.ebinterface.v41.Ebi41ItemListType;
import com.phloc.ebinterface.v41.Ebi41ListLineItemType;
import com.phloc.ebinterface.v41.Ebi41OrderReferenceDetailType;
import com.phloc.ebinterface.v41.Ebi41OrderReferenceType;
import com.phloc.ebinterface.v41.Ebi41OtherTaxType;
import com.phloc.ebinterface.v41.Ebi41PaymentConditionsType;
import com.phloc.ebinterface.v41.Ebi41PaymentMethodType;
import com.phloc.ebinterface.v41.Ebi41PaymentReferenceType;
import com.phloc.ebinterface.v41.Ebi41PeriodType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeBaseType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeDetailsType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeListLineItemDetailsType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeType;
import com.phloc.ebinterface.v41.Ebi41TaxRateType;
import com.phloc.ebinterface.v41.Ebi41TaxType;
import com.phloc.ebinterface.v41.Ebi41UnitPriceType;
import com.phloc.ebinterface.v41.Ebi41UnitType;
import com.phloc.ebinterface.v41.Ebi41UniversalBankTransactionType;
import com.phloc.ebinterface.v41.Ebi41VATItemType;
import com.phloc.ebinterface.v41.Ebi41VATType;
import com.phloc.ebinterface.v41.ObjectFactory;
import com.phloc.ubl20.codelist.EPaymentMeansCode20;
import com.phloc.ubl20.codelist.EUnitOfMeasureCode20;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.peppol.codelist.ETaxSchemeID;

/**
 * Main converter between UBL 2.0 invoice and ebInterface 4.1 invoice.
 * 
 * @author philip
 */
@Immutable
public final class InvoiceToEbInterface41Converter extends AbstractInvoiceConverter
{
  public static final int PAYMENT_REFERENCE_MAX_LENGTH = 35;

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
  public InvoiceToEbInterface41Converter (@Nonnull final Locale aDisplayLocale,
                                              @Nonnull final Locale aContentLocale,
                                              final boolean bStrictERBMode)
  {
    super (aDisplayLocale, aContentLocale, bStrictERBMode);
  }

  private void _setAddressData (@Nullable final AddressType aUBLAddress,
                                @Nonnull final Ebi41AddressType aEbiAddress,
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
        final Ebi41CountryType aEbiCountry = new Ebi41CountryType ();
        final String sCountryCode = StringHelper.trim (aUBLAddress.getCountry ().getIdentificationCodeValue ());
        Ebi41CountryCodeType eEbiCountryCode = null;
        try
        {
          eEbiCountryCode = Ebi41CountryCodeType.fromValue (sCountryCode);
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
  private Ebi41AddressType _convertParty (@Nonnull final PartyType aUBLParty,
                                          @Nonnull final String sPartyType,
                                          @Nonnull final ErrorList aTransformationErrorList)
  {
    final Ebi41AddressType aEbiAddress = new Ebi41AddressType ();

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
    final PersonType aUBLPerson = aUBLParty.getPerson ();
    if (aUBLPerson != null)
    {
      aEbiAddress.setContact (StringHelper.getImplodedNonEmpty (' ',
                                                                StringHelper.trim (aUBLPerson.getTitleValue ()),
                                                                StringHelper.trim (aUBLPerson.getFirstNameValue ()),
                                                                StringHelper.trim (aUBLPerson.getMiddleNameValue ()),
                                                                StringHelper.trim (aUBLPerson.getFamilyNameValue ()),
                                                                StringHelper.trim (aUBLPerson.getNameSuffixValue ())));
    }

    // GLN and DUNS number
    if (aUBLParty.getEndpointID () != null)
    {
      final String sEndpointID = StringHelper.trim (aUBLParty.getEndpointIDValue ());
      if (StringHelper.hasText (sEndpointID))
      {
        // We have an endpoint ID

        // Check all identifier types
        final String sSchemeIDToSearch = StringHelper.trim (aUBLParty.getEndpointID ().getSchemeID ());

        for (final Ebi41AddressIdentifierTypeType eType : Ebi41AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (sSchemeIDToSearch))
          {
            final Ebi41AddressIdentifierType aEbiType = new Ebi41AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setValue (sEndpointID);
            aEbiAddress.getAddressIdentifier ().add (aEbiType);
          }

        if (aEbiAddress.hasNoAddressIdentifierEntries ())
          aTransformationErrorList.addWarning (sPartyType,
                                               EText.PARTY_UNSUPPORTED_ENDPOINT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                        sEndpointID,
                                                                                                        aUBLParty.getEndpointID ()
                                                                                                                 .getSchemeID ()));
      }
    }

    if (aEbiAddress.hasNoAddressIdentifierEntries ())
    {
      // check party identification
      int nPartyIdentificationIndex = 0;
      for (final PartyIdentificationType aUBLPartyID : aUBLParty.getPartyIdentification ())
      {
        final String sUBLPartyID = StringHelper.trim (aUBLPartyID.getIDValue ());
        for (final Ebi41AddressIdentifierTypeType eType : Ebi41AddressIdentifierTypeType.values ())
          if (eType.value ().equalsIgnoreCase (aUBLPartyID.getID ().getSchemeID ()))
          {
            // Add GLN/DUNS number
            final Ebi41AddressIdentifierType aEbiType = new Ebi41AddressIdentifierType ();
            aEbiType.setAddressIdentifierType (eType);
            aEbiType.setValue (sUBLPartyID);
            aEbiAddress.getAddressIdentifier ().add (aEbiType);
          }
        if (aEbiAddress.hasNoAddressIdentifierEntries ())
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
    return StringHelper.getLength (sChecksum) <= 4;
  }

  /**
   * Main conversion method to convert from UBL 2.0 to ebInterface 4.1
   * 
   * @param aUBLInvoice
   *        The UBL invoice to be converted
   * @param aTransformationErrorList
   *        Error list. Must be empty!
   * @return The created ebInterface 4.1 document or <code>null</code> in case
   *         of a severe error.
   */
  @Nullable
  public Ebi41InvoiceType convertToEbInterface (@Nonnull final InvoiceType aUBLInvoice,
                                                @Nonnull final ErrorList aTransformationErrorList)
  {
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
    final Ebi41InvoiceType aEbiInvoice = new Ebi41InvoiceType ();
    aEbiInvoice.setGeneratingSystem (EBI_GENERATING_SYSTEM);
    aEbiInvoice.setDocumentType (Ebi41DocumentTypeType.INVOICE);

    // Cannot set the language, because the 3letter code is expected but we only
    // have the 2letter code!

    final String sUBLCurrencyCode = StringHelper.trim (aUBLInvoice.getDocumentCurrencyCodeValue ());
    try
    {
      aEbiInvoice.setInvoiceCurrency (Ebi41CurrencyType.fromValue (sUBLCurrencyCode));
    }
    catch (final IllegalArgumentException ex)
    {
      aTransformationErrorList.addError ("DocumentCurrencyCode",
                                         EText.INVALID_CURRENCY_CODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             sUBLCurrencyCode));
    }

    // Invoice Number
    final String sInvoiceNumber = StringHelper.trim (aUBLInvoice.getIDValue ());
    if (StringHelper.hasNoText (sInvoiceNumber))
      aTransformationErrorList.addError ("ID", EText.MISSING_INVOICE_NUMBER.getDisplayText (m_aDisplayLocale));
    aEbiInvoice.setInvoiceNumber (sInvoiceNumber);

    // Ignore the time!
    aEbiInvoice.setInvoiceDate (aUBLInvoice.getIssueDateValue ());
    if (aEbiInvoice.getInvoiceDate () == null)
      aTransformationErrorList.addError ("IssueDate", EText.MISSING_INVOICE_DATE.getDisplayText (m_aDisplayLocale));

    // Biller/Supplier (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLInvoice.getAccountingSupplierParty ();
      final Ebi41BillerType aEbiBiller = new Ebi41BillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiBiller.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiBiller.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.1
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
      aEbiInvoice.setBiller (aEbiBiller);
    }

    // Invoice recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLInvoice.getAccountingCustomerParty ();
      final Ebi41InvoiceRecipientType aEbiRecipient = new Ebi41InvoiceRecipientType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLCustomer.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiRecipient.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiRecipient.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.1
        aTransformationErrorList.addError ("AccountingCustomerParty/PartyTaxScheme",
                                           EText.SUPPLIER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null)
      {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        aEbiRecipient.setBillersInvoiceRecipientID (StringHelper.trim (aUBLCustomer.getSupplierAssignedAccountIDValue ()));
      }
      // BillersInvoiceRecipientID is no longer mandatory in ebi 4.1
      aEbiRecipient.setAddress (_convertParty (aUBLCustomer.getParty (),
                                               "AccountingCustomerParty",
                                               aTransformationErrorList));
      aEbiInvoice.setInvoiceRecipient (aEbiRecipient);
    }

    // Order reference of invoice recipient
    String sUBLOrderReferenceID = null;
    {
      final OrderReferenceType aUBLOrderReference = aUBLInvoice.getOrderReference ();
      if (aUBLOrderReference != null)
      {
        // Use directly from order reference
        sUBLOrderReferenceID = StringHelper.trim (aUBLOrderReference.getIDValue ());
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
      }

      final Ebi41OrderReferenceType aEbiOrderReference = new Ebi41OrderReferenceType ();
      aEbiOrderReference.setOrderID (sUBLOrderReferenceID);
      aEbiInvoice.getInvoiceRecipient ().setOrderReference (aEbiOrderReference);

      // Add contract reference as further identification
      for (final DocumentReferenceType aDocumentReference : aUBLInvoice.getContractDocumentReference ())
        if (StringHelper.hasTextAfterTrim (aDocumentReference.getIDValue ()))
        {
          final Ebi41FurtherIdentificationType aEbiFurtherIdentification = new Ebi41FurtherIdentificationType ();
          aEbiFurtherIdentification.setIdentificationType ("Contract");
          aEbiFurtherIdentification.setValue (StringHelper.trim (aDocumentReference.getIDValue ()));
          aEbiInvoice.getInvoiceRecipient ().getFurtherIdentification ().add (aEbiFurtherIdentification);
        }
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final Ebi41TaxType aEbiTax = new Ebi41TaxType ();
    final Ebi41VATType aEbiVAT = new Ebi41VATType ();
    {
      int nTaxTotalIndex = 0;
      for (final TaxTotalType aUBLTaxTotal : aUBLInvoice.getTaxTotal ())
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
              aUBLPercentage = aUBLTaxAmount.multiply (CGlobal.BIGDEC_100).divide (aUBLTaxableAmount,
                                                                                   SCALE_PERC,
                                                                                   ROUNDING_MODE);
            }
          }

          BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmountValue ();
          if (aUBLTaxableAmount == null && aUBLPercentage != null)
          {
            // Calculate (inexact) subtotal
            aUBLTaxableAmount = aUBLSubtotal.getTaxAmountValue ()
                                            .multiply (CGlobal.BIGDEC_100)
                                            .divide (aUBLPercentage, SCALE_PRICE_LINE, ROUNDING_MODE);
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
                  final Ebi41VATItemType aEbiVATItem = new Ebi41VATItemType ();
                  // Base amount
                  aEbiVATItem.setTaxedAmount (aUBLTaxableAmount);
                  // tax rate
                  final Ebi41TaxRateType aEbiVATTaxRate = new Ebi41TaxRateType ();
                  // Optional
                  if (false)
                    aEbiVATTaxRate.setTaxCode (sUBLTaxCategoryID);
                  aEbiVATTaxRate.setValue (aUBLPercentage);
                  aEbiVATItem.setTaxRate (aEbiVATTaxRate);
                  // Tax amount (mandatory)
                  aEbiVATItem.setAmount (aUBLSubtotal.getTaxAmountValue ());
                  // Add to list
                  aEbiVAT.getVATItem ().add (aEbiVATItem);
                }
              }
              else
              {
                // Other TAX
                final Ebi41OtherTaxType aOtherTax = new Ebi41OtherTaxType ();
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
      aEbiInvoice.setTax (aEbiTax);
    }

    // Line items
    BigDecimal aTotalZeroPercLineExtensionAmount = BigDecimal.ZERO;
    {
      final Ebi41DetailsType aEbiDetails = new Ebi41DetailsType ();
      final Ebi41ItemListType aEbiItemList = new Ebi41ItemListType ();
      int nInvoiceLineIndex = 0;
      for (final InvoiceLineType aUBLInvoiceLine : aUBLInvoice.getInvoiceLine ())
      {
        // Try to resolve tax category
        TaxCategoryType aUBLTaxCategory = ContainerHelper.getSafe (aUBLInvoiceLine.getItem ()
                                                                                  .getClassifiedTaxCategory (), 0);
        if (aUBLTaxCategory == null)
        {
          // No direct tax category -> check if it is somewhere in the tax total
          outer: for (final TaxTotalType aUBLTaxTotal : aUBLInvoiceLine.getTaxTotal ())
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
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/Item/ClassifiedTaxCategory",
                                               EText.DETAILS_TAX_PERCENTAGE_NOT_FOUND.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                              aUBLPercent));
        }

        // Start creating ebInterface line
        final Ebi41ListLineItemType aEbiListLineItem = new Ebi41ListLineItemType ();

        // Invoice line number
        final String sUBLPositionNumber = StringHelper.trim (aUBLInvoiceLine.getIDValue ());
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (sUBLPositionNumber);
        if (aUBLPositionNumber == null)
        {
          aUBLPositionNumber = BigInteger.valueOf (nInvoiceLineIndex + 1);
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/ID",
                                               EText.DETAILS_INVALID_POSITION.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLPositionNumber,
                                                                                                      aUBLPositionNumber));
        }
        aEbiListLineItem.setPositionNumber (aUBLPositionNumber);

        // Descriptions
        for (final DescriptionType aUBLDescription : aUBLInvoiceLine.getItem ().getDescription ())
          aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLDescription.getValue ()));
        if (aEbiListLineItem.getDescription ().isEmpty ())
        {
          // Use item name as description
          final NameType aUBLName = aUBLInvoiceLine.getItem ().getName ();
          if (aUBLName != null)
            aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLName.getValue ()));
        }

        // Quantity
        final Ebi41UnitType aEbiQuantity = new Ebi41UnitType ();
        if (aUBLInvoiceLine.getInvoicedQuantity () != null)
        {
          // Unit code is optional
          if (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode () != null)
            aEbiQuantity.setUnit (StringHelper.trim (aUBLInvoiceLine.getInvoicedQuantity ().getUnitCode ().value ()));
          aEbiQuantity.setValue (aUBLInvoiceLine.getInvoicedQuantityValue ());
        }
        if (aEbiQuantity.getUnit () == null)
        {
          // ebInterface requires a quantity!
          aEbiQuantity.setUnit (EUnitOfMeasureCode20.C62.getID ());
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/InvoicedQuantity/UnitCode",
                                               EText.DETAILS_INVALID_UNIT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  aEbiQuantity.getUnit ()));
        }
        if (aEbiQuantity.getValue () == null)
        {
          aEbiQuantity.setValue (BigDecimal.ONE);
          aTransformationErrorList.addWarning ("InvoiceLine[" + nInvoiceLineIndex + "]/InvoicedQuantity",
                                               EText.DETAILS_INVALID_QUANTITY.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      aEbiQuantity.getValue ()));
        }
        aEbiListLineItem.setQuantity (aEbiQuantity);

        // Unit price
        if (aUBLInvoiceLine.getPrice () != null)
        {
          final Ebi41UnitPriceType aEbiUnitPrice = new Ebi41UnitPriceType ();
          // Unit price = priceAmount/baseQuantity (mandatory)
          final BigDecimal aUBLPriceAmount = aUBLInvoiceLine.getPrice ().getPriceAmountValue ();
          aEbiUnitPrice.setValue (aUBLPriceAmount);
          // If no base quantity is present, assume 1 (optional)
          final BigDecimal aUBLBaseQuantity = aUBLInvoiceLine.getPrice ().getBaseQuantityValue ();
          if (aUBLBaseQuantity != null)
          {
            aEbiUnitPrice.setBaseQuantity (aUBLBaseQuantity);
            if (MathHelper.isEqualToZero (aUBLBaseQuantity))
              aEbiUnitPrice.setValue (BigDecimal.ZERO);
          }
          aEbiListLineItem.setUnitPrice (aEbiUnitPrice);
        }
        else
        {
          // Unit price = lineExtensionAmount / quantity (mandatory)
          final BigDecimal aUBLLineExtensionAmount = aUBLInvoiceLine.getLineExtensionAmountValue ();
          final Ebi41UnitPriceType aEbiUnitPrice = new Ebi41UnitPriceType ();
          aEbiUnitPrice.setValue (aUBLLineExtensionAmount.divide (aEbiQuantity.getValue (),
                                                                  SCALE_PRICE_LINE,
                                                                  ROUNDING_MODE));
          aEbiListLineItem.setUnitPrice (aEbiUnitPrice);
        }

        BigDecimal aEbiUnitPriceValue = aEbiListLineItem.getUnitPrice ().getValue ();
        if (aEbiListLineItem.getUnitPrice ().getBaseQuantity () != null)
          aEbiUnitPriceValue = aEbiUnitPriceValue.divide (aEbiListLineItem.getUnitPrice ().getBaseQuantity (),
                                                          SCALE_PRICE_LINE,
                                                          ROUNDING_MODE);

        // Tax rate (mandatory)
        final Ebi41TaxRateType aEbiTaxRate = new Ebi41TaxRateType ();
        aEbiTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          // Optional
          if (false)
            aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aEbiListLineItem.setTaxRate (aEbiTaxRate);

        // Line item amount (quantity * unit price +- reduction / surcharge)
        aEbiListLineItem.setLineItemAmount (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (MathHelper.isEqualToZero (aUBLPercent))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmountValue ());

        // Order reference per line
        for (final OrderLineReferenceType aUBLOrderLineReference : aUBLInvoiceLine.getOrderLineReference ())
          if (StringHelper.hasText (aUBLOrderLineReference.getLineIDValue ()))
          {
            final Ebi41OrderReferenceDetailType aEbiOrderRefDetail = new Ebi41OrderReferenceDetailType ();

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
                aTransformationErrorList.addError ("InvoiceLine[" + nInvoiceLineIndex + "]/OrderLineReference/LineID",
                                                   EText.ORDERLINE_REF_ID_EMPTY.getDisplayText (m_aDisplayLocale));
              }
              else
              {
                aEbiOrderRefDetail.setOrderPositionNumber (sOrderPosNumber);
              }
            }
            aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderRefDetail);
            break;
          }

        // Reduction and surcharge
        if (aUBLInvoiceLine.hasAllowanceChargeEntries ())
        {
          // Start with quantity*unitPrice for base amount
          BigDecimal aEbiBaseAmount = aEbiListLineItem.getQuantity ().getValue ().multiply (aEbiUnitPriceValue);
          final Ebi41ReductionAndSurchargeListLineItemDetailsType aEbiRSDetails = new Ebi41ReductionAndSurchargeListLineItemDetailsType ();

          // ebInterface can handle only Reduction or only Surcharge
          ETriState eSurcharge = ETriState.UNDEFINED;
          for (final AllowanceChargeType aUBLAllowanceCharge : aUBLInvoiceLine.getAllowanceCharge ())
          {
            final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();

            // Remember for next item
            if (eSurcharge.isUndefined ())
              eSurcharge = ETriState.valueOf (bItemIsSurcharge);
            final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();

            final Ebi41ReductionAndSurchargeBaseType aEbiRSItem = new Ebi41ReductionAndSurchargeBaseType ();
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
              aEbiRSDetails.getReductionListLineItemOrSurchargeListLineItem ()
                           .add (new ObjectFactory ().createSurchargeListLineItem (aEbiRSItem));
              aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
            }
            else
            {
              aEbiRSDetails.getReductionListLineItemOrSurchargeListLineItem ()
                           .add (new ObjectFactory ().createReductionListLineItem (aEbiRSItem));
              aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
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

    if (aEbiVAT.hasNoVATItemEntries ())
    {
      aTransformationErrorList.addError ("InvoiceLine", EText.VAT_ITEM_MISSING.getDisplayText (m_aDisplayLocale));
      if (false)
      {
        // No default in this case
        final Ebi41VATItemType aEbiVATItem = new Ebi41VATItemType ();
        aEbiVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
        final Ebi41TaxRateType aEbiVATTaxRate = new Ebi41TaxRateType ();
        aEbiVATTaxRate.setValue (BigDecimal.ZERO);
        aEbiVATItem.setTaxRate (aEbiVATTaxRate);
        aEbiVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
        aEbiVAT.getVATItem ().add (aEbiVATItem);
      }
    }

    // Global reduction and surcharge
    if (aUBLInvoice.hasAllowanceChargeEntries ())
    {
      // Start with quantity*unitPrice for base amount
      BigDecimal aEbiBaseAmount = aUBLInvoice.getLegalMonetaryTotal ().getLineExtensionAmountValue ();
      final Ebi41ReductionAndSurchargeDetailsType aEbiRS = new Ebi41ReductionAndSurchargeDetailsType ();

      int nAllowanceChargeIndex = 0;
      for (final AllowanceChargeType aUBLAllowanceCharge : aUBLInvoice.getAllowanceCharge ())
      {
        final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();

        final Ebi41ReductionAndSurchargeType aEbiRSItem = new Ebi41ReductionAndSurchargeType ();
        // Amount is mandatory
        final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
        aEbiRSItem.setAmount (aAmount);

        // Base amount is optional
        if (aUBLAllowanceCharge.getBaseAmount () != null)
          aEbiRSItem.setBaseAmount (aUBLAllowanceCharge.getBaseAmountValue ());
        if (aEbiRSItem.getBaseAmount () == null)
          aEbiRSItem.setBaseAmount (aEbiBaseAmount);

        if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null)
        {
          // Percentage is optional
          final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ().multiply (CGlobal.BIGDEC_100);
          aEbiRSItem.setPercentage (aPerc);
        }

        Ebi41TaxRateType aEbiTaxRate = null;
        for (final TaxCategoryType aUBLTaxCategory : aUBLAllowanceCharge.getTaxCategory ())
          if (aUBLTaxCategory.getPercent () != null)
          {
            aEbiTaxRate = new Ebi41TaxRateType ();
            aEbiTaxRate.setValue (aUBLTaxCategory.getPercentValue ());
            if (false)
              aEbiTaxRate.setTaxCode (aUBLTaxCategory.getIDValue ());
            break;
          }
        if (aEbiTaxRate == null)
        {
          aTransformationErrorList.addError ("Invoice/AllowanceCharge[" + nAllowanceChargeIndex + "]",
                                             EText.ALLOWANCE_CHARGE_NO_TAXRATE.getDisplayText (m_aDisplayLocale));
          // No default in this case
          if (false)
          {
            aEbiTaxRate = new Ebi41TaxRateType ();
            aEbiTaxRate.setValue (BigDecimal.ZERO);
            aEbiTaxRate.setTaxCode (ETaxCode.NOT_TAXABLE.getID ());
          }
        }
        aEbiRSItem.setTaxRate (aEbiTaxRate);

        if (bItemIsSurcharge)
        {
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createSurcharge (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
        }
        else
        {
          aEbiRS.getReductionOrSurcharge ().add (new ObjectFactory ().createReduction (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
        }
        aEbiInvoice.setReductionAndSurchargeDetails (aEbiRS);
        ++nAllowanceChargeIndex;
      }
    }

    // PrepaidAmount is not supported!
    if (aUBLInvoice.getLegalMonetaryTotal ().getPrepaidAmount () != null &&
        !MathHelper.isEqualToZero (aUBLInvoice.getLegalMonetaryTotal ().getPrepaidAmountValue ()))
    {
      aTransformationErrorList.addError ("Invoice/LegalMonetaryTotal/PrepaidAmount",
                                         EText.PREPAID_NOT_SUPPORTED.getDisplayText (m_aDisplayLocale));
    }

    // Total gross amount
    aEbiInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getTaxInclusiveAmountValue ());
    // Payable amount
    aEbiInvoice.setPayableAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmountValue ());

    // Payment method
    final Ebi41PaymentMethodType aEbiPaymentMethod = new Ebi41PaymentMethodType ();
    final Ebi41PaymentConditionsType aEbiPaymentConditions = new Ebi41PaymentConditionsType ();
    {
      int nPaymentMeansIndex = 0;
      for (final PaymentMeansType aUBLPaymentMeans : aUBLInvoice.getPaymentMeans ())
      {
        final String sPaymentMeansCode = StringHelper.trim (aUBLPaymentMeans.getPaymentMeansCodeValue ());
        final EPaymentMeansCode20 ePaymentMeans = EPaymentMeansCode20.getFromIDOrNull (sPaymentMeansCode);
        if (ePaymentMeans == EPaymentMeansCode20._31)
        {
          // Is a payment channel code present?
          final String sPaymentChannelCode = StringHelper.trim (aUBLPaymentMeans.getPaymentChannelCodeValue ());
          if (PAYMENT_CHANNEL_CODE_IBAN.equals (sPaymentChannelCode))
          {
            final Ebi41UniversalBankTransactionType aEbiUBTMethod = new Ebi41UniversalBankTransactionType ();

            // Find payment reference
            int nPaymentIDIndex = 0;
            for (final PaymentIDType aUBLPaymentID : aUBLPaymentMeans.getPaymentID ())
            {
              String sUBLPaymentID = StringHelper.trim (aUBLPaymentID.getValue ());
              if (StringHelper.hasText (sUBLPaymentID))
              {
                if (sUBLPaymentID.length () > PAYMENT_REFERENCE_MAX_LENGTH)
                {
                  // Reference
                  aTransformationErrorList.addWarning ("PaymentMeans[" +
                                                           nPaymentMeansIndex +
                                                           "]/PaymentID[" +
                                                           nPaymentIDIndex +
                                                           "]",
                                                       EText.PAYMENT_ID_TOO_LONG_CUT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                             sUBLPaymentID));
                  sUBLPaymentID = sUBLPaymentID.substring (0, PAYMENT_REFERENCE_MAX_LENGTH);
                }

                final Ebi41PaymentReferenceType aEbiPaymentReference = new Ebi41PaymentReferenceType ();
                aEbiPaymentReference.setValue (sUBLPaymentID);
                aEbiUBTMethod.setPaymentReference (aEbiPaymentReference);
              }
              ++nPaymentIDIndex;
            }

            // Beneficiary account
            final Ebi41AccountType aEbiAccount = new Ebi41AccountType ();

            // BIC
            final FinancialAccountType aUBLFinancialAccount = aUBLPaymentMeans.getPayeeFinancialAccount ();
            if (aUBLFinancialAccount.getFinancialInstitutionBranch () != null &&
                aUBLFinancialAccount.getFinancialInstitutionBranch ().getFinancialInstitution () != null)
            {
              final String sBIC = StringHelper.trim (aUBLFinancialAccount.getFinancialInstitutionBranch ()
                                                                         .getFinancialInstitution ()
                                                                         .getIDValue ());
              aEbiAccount.setBIC (sBIC);
              if (!RegExHelper.stringMatchesPattern (REGEX_BIC, sBIC))
              {
                aTransformationErrorList.addError ("PaymentMeans[" +
                                                       nPaymentMeansIndex +
                                                       "]/PayeeFinancialAccount/FinancialInstitutionBranch/FinancialInstitution/ID",
                                                   EText.BIC_INVALID.getDisplayTextWithArgs (m_aDisplayLocale, sBIC));
                aEbiAccount.setBIC (null);
              }
            }

            // IBAN
            final String sIBAN = StringHelper.trim (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue ());
            aEbiAccount.setIBAN (sIBAN);
            if (StringHelper.getLength (sIBAN) > IBAN_MAX_LENGTH)
            {
              aTransformationErrorList.addWarning ("PaymentMeans[" + nPaymentMeansIndex + "]/PayeeFinancialAccount/ID",
                                                   EText.IBAN_TOO_LONG.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                               sIBAN,
                                                                                               Integer.valueOf (IBAN_MAX_LENGTH)));
              aEbiAccount.setIBAN (sIBAN.substring (0, IBAN_MAX_LENGTH));
            }

            // Bank Account Owner - no field present - check PayeePart or
            // SupplierPartyName
            String sBankAccountOwnerName = null;
            if (aUBLInvoice.getPayeeParty () != null)
              for (final PartyNameType aPartyName : aUBLInvoice.getPayeeParty ().getPartyName ())
              {
                sBankAccountOwnerName = StringHelper.trim (aPartyName.getNameValue ());
                if (StringHelper.hasText (sBankAccountOwnerName))
                  break;
              }
            if (StringHelper.hasNoText (sBankAccountOwnerName))
            {
              final PartyType aSupplierParty = aUBLInvoice.getAccountingSupplierParty ().getParty ();
              if (aSupplierParty != null)
                for (final PartyNameType aPartyName : aSupplierParty.getPartyName ())
                {
                  sBankAccountOwnerName = StringHelper.trim (aPartyName.getNameValue ());
                  if (StringHelper.hasText (sBankAccountOwnerName))
                    break;
                }
            }
            aEbiAccount.setBankAccountOwner (sBankAccountOwnerName);

            // Comments
            if (aUBLPaymentMeans.hasInstructionNoteEntries ())
            {
              final List <String> aNotes = new ArrayList <String> ();
              for (final InstructionNoteType aUBLNote : aUBLPaymentMeans.getInstructionNote ())
              {
                final String sNote = StringHelper.trim (aUBLNote.getValue ());
                if (StringHelper.hasText (sNote))
                  aNotes.add (sNote);
              }
              if (!aNotes.isEmpty ())
                aEbiPaymentMethod.setComment (StringHelper.getImploded ('\n', aNotes));
            }

            aEbiUBTMethod.getBeneficiaryAccount ().add (aEbiAccount);
            aEbiPaymentMethod.setUniversalBankTransaction (aEbiUBTMethod);
            aEbiInvoice.setPaymentMethod (aEbiPaymentMethod);

            // Set due date (optional)
            aEbiPaymentConditions.setDueDate (aUBLPaymentMeans.getPaymentDueDateValue ());

            break;
          }

          aTransformationErrorList.addWarning ("PaymentMeans[" + nPaymentMeansIndex + "]",
                                               EText.PAYMENTMEANS_UNSUPPORTED_CHANNELCODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                                  sPaymentChannelCode));
        }
        else
          if (ePaymentMeans == EPaymentMeansCode20._49)
          {
            final Ebi41DirectDebitType aEbiDirectDebit = new Ebi41DirectDebitType ();

            if (aUBLPaymentMeans.hasInstructionNoteEntries ())
            {
              final List <String> aNotes = new ArrayList <String> ();
              for (final InstructionNoteType aUBLNote : aUBLPaymentMeans.getInstructionNote ())
              {
                final String sNote = StringHelper.trim (aUBLNote.getValue ());
                if (StringHelper.hasText (sNote))
                  aNotes.add (sNote);
              }
              if (!aNotes.isEmpty ())
                aEbiPaymentMethod.setComment (StringHelper.getImploded ('\n', aNotes));
            }

            aEbiPaymentMethod.setDirectDebit (aEbiDirectDebit);
            aEbiInvoice.setPaymentMethod (aEbiPaymentMethod);

            // Set due date (optional)
            aEbiPaymentConditions.setDueDate (aUBLPaymentMeans.getPaymentDueDateValue ());

            break;
          }
          else
          {
            aTransformationErrorList.addError ("PaymentMeans[" + nPaymentMeansIndex + "]",
                                               EText.PAYMENTMEANS_CODE_INVALID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                       EPaymentMeansCode20._31.getID (),
                                                                                                       EPaymentMeansCode20._49.getID ()));
          }

        ++nPaymentMeansIndex;
      }
    }

    if (m_bStrictERBMode)
    {
      if (aEbiInvoice.getPaymentMethod () == null)
        aTransformationErrorList.addError ("Invoice", EText.ERB_NO_PAYMENT_METHOD.getDisplayText (m_aDisplayLocale));
    }

    // Payment terms
    {
      final List <String> aPaymentConditionsNotes = new ArrayList <String> ();
      int nPaymentTermsIndex = 0;
      for (final PaymentTermsType aUBLPaymentTerms : aUBLInvoice.getPaymentTerms ())
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

            final Ebi41DiscountType aEbiDiscount = new Ebi41DiscountType ();
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
      aEbiInvoice.setPaymentConditions (aEbiPaymentConditions);
    }

    // Delivery
    final Ebi41DeliveryType aEbiDelivery = new Ebi41DeliveryType ();
    {
      int nDeliveryIndex = 0;
      for (final DeliveryType aUBLDelivery : aUBLInvoice.getDelivery ())
      {
        if (aUBLDelivery.getActualDeliveryDate () != null)
        {
          // Use the first delivery with a delivery date
          aEbiDelivery.setDate (aUBLDelivery.getActualDeliveryDateValue ());

          // Address
          if (aUBLDelivery.getDeliveryLocation () != null && aUBLDelivery.getDeliveryLocation ().getAddress () != null)
          {
            final Ebi41AddressType aEbiAddress = new Ebi41AddressType ();
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
                aUBLInvoice.getAccountingCustomerParty () != null &&
                aUBLInvoice.getAccountingCustomerParty ().getParty () != null)
            {
              for (final PartyNameType aUBLPartyName : aUBLInvoice.getAccountingCustomerParty ()
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
        final PeriodType aUBLInvoicePeriod = ContainerHelper.getSafe (aUBLInvoice.getInvoicePeriod (), 0);
        if (aUBLInvoicePeriod != null)
        {
          final XMLGregorianCalendar aStartDate = aUBLInvoicePeriod.getStartDateValue ();
          final XMLGregorianCalendar aEndDate = aUBLInvoicePeriod.getEndDateValue ();
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
              final Ebi41PeriodType aEbiPeriod = new Ebi41PeriodType ();
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
        aTransformationErrorList.addError ("Invoice", EText.ERB_NO_DELIVERY_DATE.getDisplayText (m_aDisplayLocale));
    }

    if (aEbiDelivery.getDate () != null || aEbiDelivery.getPeriod () != null)
      aEbiInvoice.setDelivery (aEbiDelivery);

    return aEbiInvoice;
  }
}
