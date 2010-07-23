/* MyFlowComposer.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 20, 2009 2:08:11 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwfdemo2;

import org.zkoss.zul.api.Listbox;
import org.zkoss.zul.api.Paging;
import org.zkoss.zul.api.Textbox;

import org.zkoss.zwf.GenericFlowComposer;
import org.zkoss.zwfdemo2.samples.booking.Booking;
import org.zkoss.zwfdemo2.samples.booking.BookingService;
import org.zkoss.zwfdemo2.samples.booking.Hotel;
import org.zkoss.zwfdemo2.samples.booking.SearchCriteria;
import org.zkoss.zwfdemo2.samples.booking.User;

/**
 * Demo of FlowComposer. 
 * @author henrichen
 *
 */
public class MyFlowComposer extends GenericFlowComposer {
	//auto wire
	private BookingService bookingService;
	private SearchCriteria searchCriteria;
	private Hotel hotel;
	private User currentUser;
	
	private Textbox searchString;
	private Listbox pageSize;
	private Paging paging;
	

	//onEntry of "main" <flow/>
	public void onEntry$main() {
		//TODO login is not implemented yet
		if (!sessionScope.containsKey("currentUser")) {
			sessionScope.put("currentUser", new User("Keith", null, "keith"));
		}
		currentUser = (User) sessionScope.get("currentUser");
		searchCriteria = new SearchCriteria();
		flowScope.put("searchCriteria", searchCriteria);
	}
	
	//onEntry of "enterSearchCriteria" <view-state/> 
	public void onEntry$enterSearchCriteria() {
		stateScope.put("bookings", bookingService.findBookings(currentUser.getName()));
	}
	
	//onTransit of "search" <transition/> of "enterSearchCriteria" <view-state/>
	public void onTransit$search$enterSearchCriteria() {
		refreshPage();
	}
	
	//onTransit of "cancelBooking" <transition/> of "enterSearchCriteria" <view-state/>
	public void onTransit$cancelBooking$enterSearchCriteria() {
		bookingService.cancelBooking((Booking) componentScope.get("booking"));
	}
	
	//onEntry of "reviewHotels" <view-state/>
	public void onEntry$reviewHotels() {
		stateScope.put("hotels", bookingService.findHotels(searchCriteria)); 
		stateScope.put("hotelsCount", bookingService.findHotelsCount(searchCriteria));
	}
	
	//onTransit of "sort" <transition/> of "reviewHotels" <view-state/>
	public void onTransit$sort$reviewHotels() {
		searchCriteria.setSortBy((String)componentScope.get("sortBy"));
	}
	
	//onTransit of "paging" <transition/> of "reviewHotels" <view-state/>
	public void onTransit$paging$reviewHotels() {
		searchCriteria.setPage(paging.getActivePage()); 
	}
	
	//onTransit of "select" <transition/> of "reviewHotels" <view-state/>
	public void onTransit$select$reviewHotels() {
		hotel = (Hotel) componentScope.get("hotel");
		flowScope.put("hotel", hotel);
	}

	//onEntry of "bookHotel" <subflow-state/>
	public void onEntry$bookHotel() {
		flowScope.put("hotelId", hotel.getId());	
	}
	
	//onTransit of "search" <transition/> of "changeSearchCriteria" <view-state/>
	public void onTransit$search$changeSearchCriteria() {
		refreshPage();
	}
	
	private void refreshPage() {
		searchCriteria.setSearchString(searchString.getValue()); 
		searchCriteria.setPageSize(((Integer)pageSize.getSelectedItemApi().getValue()).intValue()); 
		searchCriteria.resetPage();
	}
	
	//-- subflow bookingHotel --//
	//onEntry of "bookingHotel" flow
	public void onEntry$bookingHotel() {
		flowScope.put("booking", bookingService.createBooking((Long)getVariable("hotelId"), currentUser.getName()));
	}
	
	public void onTransit$no$agreeBooking() {
		flashScope.put("errmsg", "You must check to confirm!");
	}

	public void onExit$bookingConfirmed() {
		bookingService.commit((Booking) getVariable("booking"));
	}
}
