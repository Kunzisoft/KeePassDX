package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERUTCTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class Time
    extends ASN1Encodable
    implements ASN1Choice
{
    DERObject   time;

    public static Time getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject()); // must be explicitly tagged
    }

    public Time(
        DERObject   time)
    {
        if (!(time instanceof DERUTCTime)
            && !(time instanceof DERGeneralizedTime))
        {
            throw new IllegalArgumentException("unknown object passed to Time");
        }

        this.time = time; 
    }

    /**
     * creates a time object from a given date - if the date is between 1950
     * and 2049 a UTCTime object is generated, otherwise a GeneralizedTime
     * is used.
     */
    public Time(
        Date    date)
    {
        SimpleTimeZone      tz = new SimpleTimeZone(0, "Z");
        SimpleDateFormat    dateF = new SimpleDateFormat("yyyyMMddHHmmss");

        dateF.setTimeZone(tz);

        String  d = dateF.format(date) + "Z";
        int     year = Integer.parseInt(d.substring(0, 4));

        if (year < 1950 || year > 2049)
        {
            time = new DERGeneralizedTime(d);
        }
        else
        {
            time = new DERUTCTime(d.substring(2));
        }
    }

    public static Time getInstance(
        Object  obj)
    {
        if (obj instanceof Time)
        {
            return (Time)obj;
        }
        else if (obj instanceof DERUTCTime)
        {
            return new Time((DERUTCTime)obj);
        }
        else if (obj instanceof DERGeneralizedTime)
        {
            return new Time((DERGeneralizedTime)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public String getTime()
    {
        if (time instanceof DERUTCTime)
        {
            return ((DERUTCTime)time).getAdjustedTime();
        }
        else
        {
            return ((DERGeneralizedTime)time).getTime();
        }
    }

    public Date getDate()
    {
        try
        {
            if (time instanceof DERUTCTime)
            {
                return ((DERUTCTime)time).getAdjustedDate();
            }
            else
            {
                return ((DERGeneralizedTime)time).getDate();
            }
        }
        catch (ParseException e)
        {         // this should never happen
            throw new IllegalStateException("invalid date string: " + e.getMessage());
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * Time ::= CHOICE {
     *             utcTime        UTCTime,
     *             generalTime    GeneralizedTime }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return time;
    }

    public String toString()
    {
        return getTime();
    }
}
