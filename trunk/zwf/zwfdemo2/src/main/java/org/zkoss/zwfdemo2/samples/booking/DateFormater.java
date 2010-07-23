package org.zkoss.zwfdemo2.samples.booking;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.zkoss.util.Locales;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.databind.TypeConverter;

public class DateFormater {
	public static String format(Date date, String format) {
		final DateFormat df = new SimpleDateFormat(format, Locales.getCurrent());
		final TimeZone tz = TimeZone.getTimeZone("EST");
		df.setTimeZone(tz);
		return df.format(date);
	}

}
