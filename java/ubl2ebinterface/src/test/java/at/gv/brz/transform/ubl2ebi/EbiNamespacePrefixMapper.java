package at.gv.brz.transform.ubl2ebi;

import com.phloc.commons.xml.CXML;
import com.phloc.ebinterface.CEbInterface;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class EbiNamespacePrefixMapper extends NamespacePrefixMapper
{
  @Override
  public String getPreferredPrefix (final String sNamespaceUri, final String sSuggestion, final boolean requirePrefix)
  {
    // XSI prefix
    if (sNamespaceUri.equals (CXML.XML_NS_XSI))
      return "xsi";

    // XS prefix
    if (sNamespaceUri.equals (CXML.XML_NS_XSD))
      return "xs";

    // ebInterface specific prefixes
    if (sNamespaceUri.equals (CEbInterface.EBINTERFACE_40_NS) || sNamespaceUri.equals (CEbInterface.EBINTERFACE_41_NS))
      return "eb";

    return sSuggestion;
  }
}
