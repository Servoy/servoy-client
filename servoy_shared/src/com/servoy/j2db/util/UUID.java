/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.util;

import java.io.Serializable;
import java.security.SecureRandom;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "UUID")
public final class UUID implements Serializable, Comparable<UUID>, IJavaScriptType
{

	public UUID() // for json deserialisation
	{
	}

	/*
	 * The most significant 64 bits of this UUID.
	 *
	 * @serial
	 */
	private long mostSignificantBits;

	/*
	 * The least significant 64 bits of this UUID.
	 *
	 * @serial
	 */
	private long leastSignificantBits;

	/*
	 * The random number generator used by this class to create random based UUIDs.
	 */
	private static volatile SecureRandom numberGenerator = null;

	// Constructors and Factories

	/*
	 * Private constructor which uses a byte array to construct the new UUID.
	 */
	public UUID(byte[] data)
	{
		long msb = 0;
		long lsb = 0;
		if (data.length != 16)
		{
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < 8; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		for (int i = 8; i < 16; i++)
			lsb = (lsb << 8) | (data[i] & 0xff);
		this.mostSignificantBits = msb;
		this.leastSignificantBits = lsb;
	}

	/**
	 * Constructs a new <tt>UUID</tt> using the specified data. <tt>mostSigBits</tt> is used for the most significant 64 bits of the <tt>UUID</tt> and
	 * <tt>leastSigBits</tt> becomes the least significant 64 bits of the <tt>UUID</tt>.
	 *
	 * @param mostSigBits
	 * @param leastSigBits
	 */
	public UUID(long mostSigBits, long leastSigBits)
	{
		this.mostSignificantBits = mostSigBits;
		this.leastSignificantBits = leastSigBits;
	}

	/**
	 * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
	 *
	 * The <code>UUID</code> is generated using a cryptographically strong pseudo random number generator.
	 *
	 * @return a randomly generated <tt>UUID</tt>.
	 */
	public static UUID randomUUID()
	{
		SecureRandom ng = numberGenerator;
		if (ng == null)
		{
			numberGenerator = ng = new SecureRandom();
		}

		byte[] randomBytes = new byte[16];
		ng.nextBytes(randomBytes);
		randomBytes[6] &= 0x0f; /* clear version */
		randomBytes[6] |= 0x40; /* set to version 4 */
		randomBytes[8] &= 0x3f; /* clear variant */
		randomBytes[8] |= 0x80; /* set to IETF variant */
		return new UUID(randomBytes);
	}

	/**
	 * Creates a <tt>UUID</tt> from the string standard representation as described in the {@link #toString} method.
	 *
	 * @param name a string that specifies a <tt>UUID</tt>.
	 * @return a <tt>UUID</tt> with the specified value.
	 * @throws IllegalArgumentException if name does not conform to the string representation as described in {@link #toString}.
	 */
	public static UUID fromString(String name)
	{
		String[] components = name.split("-"); //$NON-NLS-1$
		if (components.length != 5) throw new IllegalArgumentException("Invalid UUID string: " + name); //$NON-NLS-1$
		for (int i = 0; i < 5; i++)
			components[i] = "0x" + components[i]; //$NON-NLS-1$

		long mostSigBits = Long.decode(components[0]).longValue();
		mostSigBits <<= 16;
		mostSigBits |= Long.decode(components[1]).longValue();
		mostSigBits <<= 16;
		mostSigBits |= Long.decode(components[2]).longValue();

		long leastSigBits = Long.decode(components[3]).longValue();
		leastSigBits <<= 48;
		leastSigBits |= Long.decode(components[4]).longValue();

		return new UUID(mostSigBits, leastSigBits);
	}

	// Field Accessor Methods

	/**
	 * Returns the least significant 64 bits of this UUID's 128 bit value.
	 *
	 * @return the least significant 64 bits of this UUID's 128 bit value.
	 */
	public long getLeastSignificantBits()
	{
		return leastSignificantBits;
	}

	/**
	 * Returns the most significant 64 bits of this UUID's 128 bit value.
	 *
	 * @return the most significant 64 bits of this UUID's 128 bit value.
	 */
	public long getMostSignificantBits()
	{
		return mostSignificantBits;
	}

	public void setLeastSignificantBits(long leastSignificantBits) // for json deserialisation
	{
		this.leastSignificantBits = leastSignificantBits;
	}

	public void setMostSignificantBits(long mostSignificantBits) // for json deserialisation
	{
		this.mostSignificantBits = mostSignificantBits;
	}

	/**
	 * The version number associated with this <tt>UUID</tt>. The version number describes how this <tt>UUID</tt> was generated.
	 *
	 * The version number has the following meaning:
	 * <p>
	 * <ul>
	 * <li>1 Time-based UUID
	 * <li>2 DCE security UUID
	 * <li>3 Name-based UUID
	 * <li>4 Randomly generated UUID
	 * </ul>
	 *
	 * @return the version number of this <tt>UUID</tt>.
	 */
	public int version()
	{
		return (int)((mostSignificantBits >> 12) & 0x0f);
	}

	/**
	 * The variant number associated with this <tt>UUID</tt>. The variant number describes the layout of the <tt>UUID</tt>.
	 *
	 * The variant number has the following meaning:
	 * <p>
	 * <ul>
	 * <li>0 Reserved for NCS backward compatibility
	 * <li>2 The Leach-Salz variant (used by this class)
	 * <li>6 Reserved, Microsoft Corporation backward compatibility
	 * <li>7 Reserved for future definition
	 * </ul>
	 *
	 * @return the variant number of this <tt>UUID</tt>.
	 */
	public int variant()
	{
		// This field is composed of a varying number of bits
		if ((leastSignificantBits >>> 63) == 0)
		{
			return 0;
		}
		else if ((leastSignificantBits >>> 62) == 2)
		{
			return 2;
		}
		else
		{
			return (int)(leastSignificantBits >>> 61);
		}
	}

	/**
	 * The timestamp value associated with this UUID.
	 *
	 * <p>
	 * The 60 bit timestamp value is constructed from the time_low, time_mid, and time_hi fields of this <tt>UUID</tt>. The resulting timestamp is measured in
	 * 100-nanosecond units since midnight, October 15, 1582 UTC.
	 * <p>
	 *
	 * The timestamp value is only meaningful in a time-based UUID, which has version type 1. If this <tt>UUID</tt> is not a time-based UUID then this method
	 * throws UnsupportedOperationException.
	 *
	 * @throws UnsupportedOperationException if this UUID is not a version 1 UUID.
	 */
	public long timestamp()
	{
		if (version() != 1)
		{
			throw new UnsupportedOperationException("Not a time-based UUID"); //$NON-NLS-1$
		}

		long result = (mostSignificantBits & 0x0000000000000FFFL) << 48;
		result |= ((mostSignificantBits >> 16) & 0xFFFFL) << 32;
		result |= mostSignificantBits >>> 32;
		return result;
	}

	/**
	 * The clock sequence value associated with this UUID.
	 *
	 * <p>
	 * The 14 bit clock sequence value is constructed from the clock sequence field of this UUID. The clock sequence field is used to guarantee temporal
	 * uniqueness in a time-based UUID.
	 * <p>
	 *
	 * The clockSequence value is only meaningful in a time-based UUID, which has version type 1. If this UUID is not a time-based UUID then this method throws
	 * UnsupportedOperationException.
	 *
	 * @return the clock sequence of this <tt>UUID</tt>.
	 * @throws UnsupportedOperationException if this UUID is not a version 1 UUID.
	 */
	public int clockSequence()
	{
		if (version() != 1)
		{
			throw new UnsupportedOperationException("Not a time-based UUID"); //$NON-NLS-1$
		}
		return (int)((leastSignificantBits & 0x3FFF000000000000L) >>> 48);
	}

	/**
	 * The node value associated with this UUID.
	 *
	 * <p>
	 * The 48 bit node value is constructed from the node field of this UUID. This field is intended to hold the IEEE 802 address of the machine that generated
	 * this UUID to guarantee spatial uniqueness.
	 * <p>
	 *
	 * The node value is only meaningful in a time-based UUID, which has version type 1. If this UUID is not a time-based UUID then this method throws
	 * UnsupportedOperationException.
	 *
	 * @return the node value of this <tt>UUID</tt>.
	 * @throws UnsupportedOperationException if this UUID is not a version 1 UUID.
	 */
	public long node()
	{
		if (version() != 1)
		{
			throw new UnsupportedOperationException("Not a time-based UUID"); //$NON-NLS-1$
		}
		return leastSignificantBits & 0x0000FFFFFFFFFFFFL;
	}

	// Object Inherited Methods

	/**
	 * Returns a <code>String</code> object representing this <code>UUID</code>.
	 *
	 * <p>
	 * The UUID string representation is as described by this BNF :
	 *
	 * <pre>
	 *     UUID                   = &lt;time_low&gt; &quot;-&quot; &lt;time_mid&gt; &quot;-&quot;
	 *                              &lt;time_high_and_version&gt; &quot;-&quot;
	 *                              &lt;variant_and_sequence&gt; &quot;-&quot;
	 *                              &lt;node&gt;
	 *     time_low               = 4*&lt;hexOctet&gt;
	 *     time_mid               = 2*&lt;hexOctet&gt;
	 *     time_high_and_version  = 2*&lt;hexOctet&gt;
	 *     variant_and_sequence   = 2*&lt;hexOctet&gt;
	 *     node                   = 6*&lt;hexOctet&gt;
	 *     hexOctet               = &lt;hexDigit&gt;&lt;hexDigit&gt;
	 *     hexDigit               =
	 *           &quot;0&quot; | &quot;1&quot; | &quot;2&quot; | &quot;3&quot; | &quot;4&quot; | &quot;5&quot; | &quot;6&quot; | &quot;7&quot; | &quot;8&quot; | &quot;9&quot;
	 *           | &quot;a&quot; | &quot;b&quot; | &quot;c&quot; | &quot;d&quot; | &quot;e&quot; | &quot;f&quot;
	 *           | &quot;A&quot; | &quot;B&quot; | &quot;C&quot; | &quot;D&quot; | &quot;E&quot; | &quot;F&quot;
	 * </pre>
	 *
	 * @return a string representation of this <tt>UUID</tt>.
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(36);
		sb.append(digits(mostSignificantBits >> 32, 8));
		sb.append('-');
		sb.append(digits(mostSignificantBits >> 16, 4));
		sb.append('-');
		sb.append(digits(mostSignificantBits, 4));
		sb.append('-');
		sb.append(digits(leastSignificantBits >> 48, 4));
		sb.append('-');
		sb.append(digits(leastSignificantBits, 12));
		return sb.toString().toUpperCase();
	}

	/** Returns val represented by the specified number of hex digits. */
	private static String digits(long val, int digits)
	{
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	/**
	 * Returns a hash code for this <code>UUID</code>.
	 *
	 * @return a hash code value for this <tt>UUID</tt>.
	 */
	@Override
	public int hashCode()
	{
		return (int)((mostSignificantBits >> 32) ^ mostSignificantBits ^ (leastSignificantBits >> 32) ^ leastSignificantBits);
	}

	/**
	 * Compares this object to the specified object. The result is <tt>true</tt> if and only if the argument is not <tt>null</tt>, is a <tt>UUID</tt> object,
	 * has the same variant, and contains the same value, bit for bit, as this <tt>UUID</tt>.
	 *
	 * @param obj the object to compare with.
	 * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof UUID)
		{
			UUID id = (UUID)obj;
			return (mostSignificantBits == id.mostSignificantBits && leastSignificantBits == id.leastSignificantBits);
		}
		return false;
	}

	// Comparison Operations

	/**
	 * Compares this UUID with the specified UUID.
	 *
	 * <p>
	 * The first of two UUIDs follows the second if the most significant field in which the UUIDs differ is greater for the first UUID.
	 *
	 * @param otherUUID <tt>UUID</tt> to which this <tt>UUID</tt> is to be compared.
	 * @return -1, 0 or 1 as this <tt>UUID</tt> is less than, equal to, or greater than <tt>other</tt>.
	 */
	public int compareTo(UUID otherUUID)
	{
		// The ordering is intentionally set up so that the UUIDs
		// can simply be numerically compared as two numbers
		return (this.mostSignificantBits < otherUUID.mostSignificantBits ? -1 : (this.mostSignificantBits > otherUUID.mostSignificantBits ? 1
			: (this.leastSignificantBits < otherUUID.leastSignificantBits ? -1 : (this.leastSignificantBits > otherUUID.leastSignificantBits ? 1 : 0))));
	}

	/**
	 * Convert to byte array, compatible with UUID(byte[])
	 *
	 * @return the result byte array
	 */
	public byte[] toBytes()
	{
		byte[] data = new byte[16];
		for (int i = 0; i < 8; i++)
		{
			data[i] = (byte)((mostSignificantBits >> ((7 - i) * 8)) & 0xff);
			data[8 + i] = (byte)((leastSignificantBits >> ((7 - i) * 8)) & 0xff);
		}
		return data;
	}

	/**
	 * Get the string representation of the UUID.
	 *
	 * @sample uuid.toString();
	 * @return the string representation of the UUID.
	 */
	public String js_toString()
	{
		return toString();
	}

	/**
	 * Get the byte array representation of the UUID.
	 *
	 * @sample uuid.toBytes();
	 * @return the byte array representation of the UUID.
	 */
	public byte[] js_toBytes()
	{
		return toBytes();
	}

}
