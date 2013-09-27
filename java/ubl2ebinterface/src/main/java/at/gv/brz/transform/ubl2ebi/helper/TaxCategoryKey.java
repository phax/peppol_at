package at.gv.brz.transform.ubl2ebi.helper;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.phloc.commons.hash.HashCodeGenerator;
import com.phloc.commons.string.ToStringGenerator;

/**
 * This class contains the data of a single TaxtCategory. That is required when
 * the details items don't have a percentage and the percentage values must be
 * evaluated from the tax subtotal elements.
 * 
 * @author philip
 */
@Immutable
public final class TaxCategoryKey
{
  private final SchemedID m_aTaxCategoryScheme;
  private final SchemedID m_aTaxCategoryID;

  public TaxCategoryKey (@Nonnull final SchemedID aTaxCategoryScheme, @Nonnull final SchemedID aTaxCategoryID)
  {
    if (aTaxCategoryScheme == null)
      throw new NullPointerException ("taxCategoryScheme");
    if (aTaxCategoryID == null)
      throw new NullPointerException ("taxCategoryID");
    m_aTaxCategoryScheme = aTaxCategoryScheme;
    m_aTaxCategoryID = aTaxCategoryID;
  }

  @Nonnull
  public SchemedID getTaxCategoryScheme ()
  {
    return m_aTaxCategoryScheme;
  }

  @Nonnull
  public SchemedID getTaxCategoryID ()
  {
    return m_aTaxCategoryID;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!(o instanceof TaxCategoryKey))
      return false;
    final TaxCategoryKey rhs = (TaxCategoryKey) o;
    return m_aTaxCategoryScheme.equals (rhs.m_aTaxCategoryScheme) && m_aTaxCategoryID.equals (rhs.m_aTaxCategoryID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aTaxCategoryScheme).append (m_aTaxCategoryID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("scheme", m_aTaxCategoryScheme)
                                       .append ("ID", m_aTaxCategoryID)
                                       .toString ();
  }
}
