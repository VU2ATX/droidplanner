package com.diydrones.droidplanner.fragments;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_global_position_int;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

public class FlightMapFragment extends MapFragment implements LocationSource{
	private GoogleMap mMap;
	private OnLocationChangedListener myLocationListner;
	Location droneLocation;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup,
			Bundle bundle) {
		View view = super.onCreateView(inflater, viewGroup, bundle);
		
		mMap = getMap();
		mMap.setMyLocationEnabled(true);
		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

		UiSettings mUiSettings = mMap.getUiSettings();
		mUiSettings.setMyLocationButtonEnabled(true);
		mUiSettings.setCompassEnabled(true);
		mUiSettings.setTiltGesturesEnabled(false);
		mMap.setLocationSource(this);

		droneLocation = new Location("");
		droneLocation.setAccuracy(10);
		return view;
	}

	public void updateDronePosition(float heading, LatLng coord) {
		droneLocation.setLatitude(coord.latitude);
		droneLocation.setLongitude(coord.longitude);
		droneLocation.setBearing(heading);
		
		myLocationListner.onLocationChanged(droneLocation);
	}

	public void receiveData(MAVLinkMessage msg) {
		if(msg.msgid == msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT) {
			LatLng position = new LatLng(((msg_global_position_int)msg).lat/1E7, ((msg_global_position_int)msg).lon/1E7);
			float heading = (0x0000FFFF & ((int)((msg_global_position_int)msg).hdg))/100f; // TODO fix unsigned short read at mavlink library
			updateDronePosition(heading, position);
		}
	}

	@Override
	public void activate(OnLocationChangedListener listner) {
		myLocationListner  = listner;		
	}

	@Override
	public void deactivate() {		
	}

}
