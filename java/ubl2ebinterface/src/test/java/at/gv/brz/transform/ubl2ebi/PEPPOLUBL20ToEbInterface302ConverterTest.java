package at.gv.brz.transform.ubl2ebi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import at.peppol.test.ETestFileType;
import at.peppol.test.TestFiles;

import com.phloc.commons.io.IReadableResource;
import com.phloc.commons.xml.serialize.XMLReader;
import com.phloc.ebinterface.EbInterface302Marshaller;
import com.phloc.ubl.UBL20DocumentMarshaller;

/**
 * Test class for class {@link PEPPOLUBL20ToEbInterface302Converter}.
 * 
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class PEPPOLUBL20ToEbInterface302ConverterTest {
  @Test
  public void testConvertPEPPOLInvoice () throws SAXException {
    // For all PEPPOL test invoices
    for (final IReadableResource aRes : TestFiles.getSuccessFiles (ETestFileType.INVOICE)) {
      System.out.println (aRes.getPath ());
      assertTrue (aRes.exists ());

      // Read XML
      final Document aDoc = XMLReader.readXMLDOM (aRes);
      assertNotNull (aDoc);

      // Read UBL
      final InvoiceType aUBLInvoice = UBL20DocumentMarshaller.readInvoice (aDoc);
      assertNotNull (aUBLInvoice);

      // Convert to ebInterface
      final com.phloc.ebinterface.v302.InvoiceType aEbInvoice = PEPPOLUBL20ToEbInterface302Converter.convertToEbInterface (aUBLInvoice);
      assertNotNull (aEbInvoice);

      // Convert ebInterface to XML
      final Document aDocEb = new EbInterface302Marshaller ().write (aEbInvoice);
      assertNotNull (aDocEb);
    }
  }
}
