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
import at.peppol.commons.identifier.docid.IPredefinedDocumentTypeIdentifier;
import at.peppol.commons.identifier.procid.IPredefinedProcessIdentifier;
import at.peppol.commons.identifier.procid.PredefinedProcessIdentifierManager;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.regex.RegExHelper;
import com.phloc.commons.string.StringHelper;
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

/**
 * Main converter between UBL 2.0 invoice and ebInterface 3.0.2 invoice.
 * 
 * @author philip
 */
@Immutable
public final class PEPPOLUBL20ToEbInterface302Converter {
  private static final Logger s_aLogger = LoggerFactory.getLogger (PEPPOLUBL20ToEbInterface302Converter.class);
  private static final String DUMMY_VALUE = "DUMMY_VALUE";
  private static final String REGEX_BIC = "[0-9 | A-Z | a-z]{8}([0-9 | A-Z | a-z]{3})?";

  private PEPPOLUBL20ToEbInterface302Converter () {}

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
    final IPredefinedProcessIdentifier aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
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
      IPredefinedDocumentTypeIdentifier aMatchingDocID = null;
      for (final IPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
        if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID)) {
          // We found a match
          aMatchingDocID = aDocID;
          break;
        }
      if (aMatchingDocID == null)
        return "Invalid CustomizationID value present!";
    }

    // Invoice type code
    InvoiceTypeCodeType aInvoiceTypeCode = aUBLInvoice.getInvoiceTypeCode ();
    if (aInvoiceTypeCode == null) {
      s_aLogger.warn ("No InvoiceTypeCode present! Setting to default.");
      aInvoiceTypeCode = new InvoiceTypeCodeType ();
      aInvoiceTypeCode.setValue (CPeppolUBL.INVOICE_TYPE_CODE);
    }
    else {
      if (!CPeppolUBL.INVOICE_TYPE_CODE.equals (aInvoiceTypeCode.getValue ()))
        return "Invalid InvoiceTypeCode value present!";
    }

    // Done
    return null;
  }

  @Nonnull
  private static String _concatenate (@Nonnull final String sSep, @Nonnull final String... aValues) {
    final StringBuilder aSB = new StringBuilder ();
    for (final String sValue : aValues)
      if (StringHelper.hasText (sValue)) {
        if (aSB.length () > 0)
          aSB.append (sSep);
        aSB.append (sValue);
      }
    return aSB.toString ();
  }

  @Nonnull
  private static AddressType _convertAddress (final ObjectFactory aObjectFactory, final PartyType aUBLParty) {
    final AddressType ret = aObjectFactory.createAddressType ();

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null) {
      ret.setName (aUBLPartyName.getName () == null ? null : aUBLPartyName.getName ().getValue ());
    }

    final oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType aUBLAddress = aUBLParty.getPostalAddress ();
    if (aUBLAddress != null) {
      ret.setStreet (_concatenate (" ",
                                   aUBLAddress.getStreetName () == null ? null : aUBLAddress.getStreetName ()
                                                                                            .getValue (),
                                   aUBLAddress.getBuildingNumber () == null ? null : aUBLAddress.getBuildingNumber ()
                                                                                                .getValue ()));
      if (aUBLAddress.getPostbox () != null)
        ret.setPOBox (aUBLAddress.getPostbox ().getValue ());
      if (aUBLAddress.getCityName () != null)
        ret.setTown (aUBLAddress.getCityName ().getValue ());
      if (aUBLAddress.getPostalZone () != null)
        ret.setZIP (aUBLAddress.getPostalZone ().getValue ());
      if (aUBLAddress.getCountry () != null)
        ret.setCountry (aUBLAddress.getCountry ().getIdentificationCode ().getValue ());
    }

    final ContactType aUBLContact = aUBLParty.getContact ();
    if (aUBLContact != null) {
      if (aUBLContact.getTelephone () != null)
        ret.setPhone (aUBLContact.getTelephone ().getValue ());
      if (aUBLContact.getElectronicMail () != null)
        ret.setEmail (aUBLContact.getElectronicMail ().getValue ());
    }

    final PersonType aUBLPerson = aUBLParty.getPerson ();
    if (aUBLPerson != null) {
      ret.setContact (_concatenate (" ",
                                    aUBLPerson.getTitle () == null ? null : aUBLPerson.getTitle ().getValue (),
                                    aUBLPerson.getFirstName () == null ? null : aUBLPerson.getFirstName ().getValue (),
                                    aUBLPerson.getMiddleName () == null ? null : aUBLPerson.getMiddleName ()
                                                                                           .getValue (),
                                    aUBLPerson.getFamilyName () == null ? null : aUBLPerson.getFamilyName ()
                                                                                           .getValue (),
                                    aUBLPerson.getNameSuffix () == null ? null : aUBLPerson.getNameSuffix ()
                                                                                           .getValue ()));
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

  private static boolean _isSupportedTaxSchemeSchemeID (final String sUBLTaxSchemeSchemeID) {
    return sUBLTaxSchemeSchemeID == null || sUBLTaxSchemeSchemeID.equals ("UN/ECE 5153");
  }

  /**
   * Main conversion method to convert from UBL 2.0 to ebInterface 3.0.2
   * 
   * @param aUBLInvoice
   *        The UBL invoice to be converted
   * @return The created ebInterface 3.0.2 document
   */
  @Nonnull
  public static InvoiceType convertToEbInterface (@Nonnull final oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType aUBLInvoice) {
    if (aUBLInvoice == null)
      throw new NullPointerException ("UBLInvoice");

    final ObjectFactory aObjectFactory = new ObjectFactory ();

    // Consistency check
    final String sConsistencyValidationResult = _checkConsistency (aUBLInvoice);
    if (sConsistencyValidationResult != null)
      throw new IllegalArgumentException ("Consistency validation failed: " + sConsistencyValidationResult);

    // Build ebInterface invoice
    final InvoiceType aNewInvoice = aObjectFactory.createInvoiceType ();
    aNewInvoice.setGeneratingSystem ("UBL 2.0 to ebInterface 3.0.2 converter");
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
        if (aPartyTaxScheme.getTaxScheme ().getID ().getValue ().equals ("VAT")) {
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
        if (aPartyTaxScheme.getTaxScheme ().getID ().getValue ().equals ("VAT")) {
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
        if (!aUBLContractDocumentReferences.isEmpty ())
          sUBLOrderReferenceID = ContainerHelper.getFirstElement (aUBLContractDocumentReferences).getID ().getValue ();
      }

      // Concatenate accounting area and main order reference for ebInterface
      // 3.x
      sOrderReferenceID = StringHelper.concatenateOnDemand (sAccountingArea, ":", sUBLOrderReferenceID);
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
          BigDecimal aUBLPercentage = aUBLTaxCategory.getPercent () != null ? aUBLTaxCategory.getPercent ().getValue ()
                                                                           : null;
          if (aUBLPercentage == null) {
            // no it is not :(
            final BigDecimal aUBLTaxAmount = aUBLSubtotal.getTaxAmount ().getValue ();
            final BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmount ().getValue ();
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

          if (_isSupportedTaxSchemeSchemeID (sUBLTaxSchemeSchemeID) && "VAT".equals (sUBLTaxSchemeID)) {
            // add VAT item
            final ItemType aNewVATItem = aObjectFactory.createItemType ();
            // Base amount
            aNewVATItem.setTaxedAmount (aUBLSubtotal.getTaxableAmount ().getValue ());
            // tax rate
            final TaxRateType aNewVATTaxRate = aObjectFactory.createTaxRateType ();
            aNewVATTaxRate.setTaxCode (sUBLTaxCategoryID);
            aNewVATTaxRate.setValue (aUBLPercentage);
            aNewVATItem.setTaxRate (aNewVATTaxRate);
            // Tax amount
            aNewVATItem.setAmount (aUBLSubtotal.getTaxAmount ().getValue ());
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
        // Try to resolve tax percentage
        TaxCategoryType aUBLTaxCategory = ContainerHelper.getSafe (aUBLInvoiceLine.getItem ()
                                                                                  .getClassifiedTaxCategory (), 0);
        if (aUBLTaxCategory == null) {
          final TaxTotalType aUBLTaxTotal = ContainerHelper.getFirstElement (aUBLInvoiceLine.getTaxTotal ());
          if (aUBLTaxTotal != null) {
            final TaxSubtotalType aUBLTaxSubTotal = ContainerHelper.getFirstElement (aUBLTaxTotal.getTaxSubtotal ());
            if (aUBLTaxSubTotal != null)
              aUBLTaxCategory = aUBLTaxSubTotal.getTaxCategory ();
          }
        }

        BigDecimal aUBLPercent = null;
        if (aUBLTaxCategory != null) {
          // Specified at tax category?
          if (aUBLTaxCategory.getPercent () != null)
            aUBLPercent = aUBLTaxCategory.getPercent ().getValue ();

          if (aUBLPercent == null) {
            // Not specified - check from previous map
            final String sUBLTaxSchemeSchemeID = aUBLTaxCategory.getTaxScheme ().getID ().getSchemeID ();
            final String sUBLTaxSchemeID = aUBLTaxCategory.getTaxScheme ().getID ().getValue ();

            final String sUBLTaxCategorySchemeID = aUBLTaxCategory.getID ().getSchemeID ();
            final String sUBLTaxCategoryID = aUBLTaxCategory.getID ().getValue ();

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
        BigInteger aUBLPositionNumber = StringHelper.parseBigInteger (aUBLInvoiceLine.getID ().getValue ());
        if (aUBLPositionNumber == null) {
          s_aLogger.warn ("Failed to parse UBL invoice line '" +
                          aUBLInvoiceLine.getID ().getValue () +
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
          aNewQuantity.setValue (aUBLInvoiceLine.getInvoicedQuantity ().getValue ());
        }
        else {
          // ebInterface requires a quantity!
          // XXX is this correct as the default?
          aNewQuantity.setUnit ("C62");
          aNewQuantity.setValue (BigDecimal.ONE);
        }
        aNewListLineItem.setQuantity (aNewQuantity);
        // Unit price
        if (aUBLInvoiceLine.getPrice () != null) {
          // Unit price = priceAmount/baseQuantity
          final BigDecimal aUBLPriceAmount = aUBLInvoiceLine.getPrice ().getPriceAmount ().getValue ();
          // If no base quantity is present, assume 1
          final BigDecimal aUBLBaseQuantity = aUBLInvoiceLine.getPrice ().getBaseQuantity () != null
                                                                                                    ? aUBLInvoiceLine.getPrice ()
                                                                                                                     .getBaseQuantity ()
                                                                                                                     .getValue ()
                                                                                                    : BigDecimal.ONE;
          aNewListLineItem.setUnitPrice (aUBLPriceAmount.divide (aUBLBaseQuantity));
        }
        else {
          // Unit price = lineExtensionAmount / quantity
          final BigDecimal aUBLLineExtensionAmount = aUBLInvoiceLine.getLineExtensionAmount ().getValue ();
          aNewListLineItem.setUnitPrice (aUBLLineExtensionAmount.divide (aNewQuantity.getValue ()));
        }

        // Tax rate (mandatory)
        final TaxRateType aNewTaxRate = aObjectFactory.createTaxRateType ();
        aNewTaxRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null && aUBLTaxCategory.getID () != null)
          aNewTaxRate.setTaxCode (aUBLTaxCategory.getID ().getValue ());
        aNewListLineItem.setTaxRate (aNewTaxRate);

        // Line item amount (quantity * unit price)
        aNewListLineItem.setLineItemAmount (aUBLInvoiceLine.getLineExtensionAmount ().getValue ());

        // Special handling in case no VAT item is present
        if (aUBLPercent.equals (BigDecimal.ZERO))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLInvoiceLine.getLineExtensionAmount ()
                                                                                                    .getValue ());

        // Order reference for detail
        final OrderLineReferenceType aOrderLineReference = ContainerHelper.getFirstElement (aUBLInvoiceLine.getOrderLineReference ());
        if (aOrderLineReference != null) {
          final OrderReferenceDetailType aNewOrderReferenceDetail = aObjectFactory.createOrderReferenceDetailType ();
          aNewOrderReferenceDetail.setOrderID (sOrderReferenceID);
          aNewOrderReferenceDetail.setOrderPositionNumber (aOrderLineReference.getLineID ().getValue ());
          aNewListLineItem.setInvoiceRecipientsOrderReference (aNewOrderReferenceDetail);
        }

        // Add the item to the list
        aNewItemList.getListLineItem ().add (aNewListLineItem);
        nInvoiceLineIndex++;
      }
      aNewDetails.getItemList ().add (aNewItemList);
      aNewInvoice.setDetails (aNewDetails);
    }

    if (aNewVAT.getItem ().isEmpty ()) {
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
    aNewInvoice.setTotalGrossAmount (aUBLInvoice.getLegalMonetaryTotal ().getPayableAmount ().getValue ());

    // Payment method
    {
      for (final PaymentMeansType aUBLPaymentMeans : aUBLInvoice.getPaymentMeans ()) {
        // Is a payment channel code present?
        if (aUBLPaymentMeans.getPaymentChannelCode () != null &&
            aUBLPaymentMeans.getPaymentChannelCode ().getValue ().equals ("IBAN")) {
          final UniversalBankTransactionType aNewUBTMethod = aObjectFactory.createUniversalBankTransactionType ();
          // Beneficiary account
          final AccountType aNewAccount = aObjectFactory.createAccountType ();

          // BIC
          aNewAccount.setBIC (aUBLPaymentMeans.getPayeeFinancialAccount ()
                                              .getFinancialInstitutionBranch ()
                                              .getFinancialInstitution ()
                                              .getID ()
                                              .getValue ());
          if (!RegExHelper.stringMatchesPattern (REGEX_BIC, aNewAccount.getBIC ())) {
            s_aLogger.error ("The BIC '" + aNewAccount.getBIC () + "' does not match the required regular expression.");
            aNewAccount.setBIC (null);
          }

          // IBAN
          aNewAccount.setIBAN (aUBLPaymentMeans.getPayeeFinancialAccount ().getID ().getValue ());
          if (StringHelper.length (aNewAccount.getIBAN ()) > 34) {
            s_aLogger.warn ("The IBAN '" + aNewAccount.getIBAN () + "' is too long and cut to 34 chars.");
            aNewAccount.setIBAN (aNewAccount.getIBAN ().substring (0, 34));
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
