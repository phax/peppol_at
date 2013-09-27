package at.gv.brz.transform.ubl2ebi.helper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.hash.HashCodeGenerator;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.ToStringGenerator;

/**
 * Represents a generic ID consisting of schemeID and the main ID value.
 * 
 * @author philip
 */
@Immutable
public final class SchemedID
{
  private final String m_sSchemeID;
  private final String m_sID;

  public SchemedID (@Nullable final String sSchemeID, @Nonnull @Nonempty final String sID)
  {
    if (StringHelper.hasNoText (sID))
      throw new IllegalArgumentException ("ID");
    m_sSchemeID = sSchemeID;
    m_sID = sID;
  }

  @Nullable
  public String getSchemeID ()
  {
    return m_sSchemeID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!(o instanceof SchemedID))
      return false;
    final SchemedID rhs = (SchemedID) o;
    return EqualsUtils.equals (m_sSchemeID, rhs.m_sSchemeID) && m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sSchemeID).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).appendIfNotNull ("schemeID", m_sSchemeID).append ("ID", m_sID).toString ();
  }
}
