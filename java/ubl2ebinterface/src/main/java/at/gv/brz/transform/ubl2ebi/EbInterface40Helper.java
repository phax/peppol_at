package at.gv.brz.transform.ubl2ebi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PersonType;
import at.gv.brz.transform.ubl2ebi.AbstractConverter.EText;

import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.locale.country.CountryCache;
import com.phloc.commons.string.StringHelper;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierType;
import com.phloc.ebinterface.v40.Ebi40AddressIdentifierTypeType;
import com.phloc.ebinterface.v40.Ebi40AddressType;
import com.phloc.ebinterface.v40.Ebi40CountryCodeType;
import com.phloc.ebinterface.v40.Ebi40CountryType;
import com.phloc.validation.error.ErrorList;

public final class EbInterface40Helper
{
  private EbInterface40Helper ()
  {}

  public static void setAddressData (@Nullable final AddressType aUBLAddress,
                                     @Nonnull final Ebi40AddressType aEbiAddress,
                                     @Nonnull final String sPartyType,
                                     @Nonnull final ErrorList aTransformationErrorList,
                                     @Nonnull final Locale aContentLocale,
                                     @Nonnull final Locale aDisplayLocale)
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
                                             EText.ADDRESS_INVALID_COUNTRY.getDisplayTextWithArgs (aDisplayLocale,
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
            aEbiCountry.setContent (aLocale.getDisplayCountry (aContentLocale));
        }
        aEbiAddress.setCountry (aEbiCountry);
      }
    }

    if (aEbiAddress.getStreet () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/StreetName",
                                         EText.ADDRESS_NO_STREET.getDisplayText (aDisplayLocale));
    if (aEbiAddress.getTown () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/CityName",
                                         EText.ADDRESS_NO_CITY.getDisplayText (aDisplayLocale));
    if (aEbiAddress.getZIP () == null)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/PostalZone",
                                         EText.ADDRESS_NO_ZIPCODE.getDisplayText (aDisplayLocale));
    if (aEbiAddress.getCountry () == null && !bCountryErrorMsgEmitted)
      aTransformationErrorList.addError (sPartyType + "/PostalAddress/Country/IdentificationCode",
                                         EText.ADDRESS_NO_COUNTRY.getDisplayText (aDisplayLocale));
  }

  @Nonnull
  public static Ebi40AddressType convertParty (@Nonnull final PartyType aUBLParty,
                                               @Nonnull final String sPartyType,
                                               @Nonnull final ErrorList aTransformationErrorList,
                                               @Nonnull final Locale aContentLocale,
                                               @Nonnull final Locale aDisplayLocale)
  {
    final Ebi40AddressType aEbiAddress = new Ebi40AddressType ();

    if (aUBLParty.getPartyNameCount () > 1)
      aTransformationErrorList.addWarning (sPartyType + "/PartyName",
                                           EText.MULTIPLE_PARTIES.getDisplayText (aDisplayLocale));

    // Convert name
    final PartyNameType aUBLPartyName = ContainerHelper.getSafe (aUBLParty.getPartyName (), 0);
    if (aUBLPartyName != null)
      aEbiAddress.setName (StringHelper.trim (aUBLPartyName.getNameValue ()));

    if (aEbiAddress.getName () == null)
      aTransformationErrorList.addError (sPartyType, EText.PARTY_NO_NAME.getDisplayText (aDisplayLocale));

    // Convert main address
    setAddressData (aUBLParty.getPostalAddress (),
                    aEbiAddress,
                    sPartyType,
                    aTransformationErrorList,
                    aContentLocale,
                    aDisplayLocale);

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
                                               EText.PARTY_UNSUPPORTED_ENDPOINT.getDisplayTextWithArgs (aDisplayLocale,
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
                                             EText.PARTY_UNSUPPORTED_ADDRESS_IDENTIFIER.getDisplayTextWithArgs (aDisplayLocale,
                                                                                                                sUBLPartyID,
                                                                                                                aUBLPartyID.getID ()
                                                                                                                           .getSchemeID ()));
        ++nPartyIdentificationIndex;
      }
    }

    return aEbiAddress;
  }
}
