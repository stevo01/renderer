package jsearch;

public class MapBB {
	public double minlat;
	public double minlon;
	public double maxlat;
	public double maxlon;
	
	public String getstring() 
	{
		String ret = String.format("%f, %f, %f, %f", minlat, minlon, maxlat, maxlon);
		return ret;
	}
	
	public String getinfo() 
	{
		String ret = String.format("(minlat, minlon, maxlat, maxlon) %f, %f, %f, %f", minlat, minlon, maxlat, maxlon);
		return ret;
	}
	
	public String geojson() 
	{
		
		String ret = "{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[";
		                      
		ret += String.format("[%f, %f],", minlon, minlat);
		ret += String.format("[%f, %f],", maxlon, minlat);
		ret += String.format("[%f, %f],", maxlon, maxlat);
		ret += String.format("[%f, %f],", minlon, maxlat);
		ret += String.format("[%f, %f] ", minlon, minlat);
		ret += "]]}, \"properties\": { }}";
		
		return ret;
	}
	
}

/*
  sample:
  minlat = 53.644638 
  minlon = 11.777344 
  maxlat = 54.265224 
  maxlon = 12.832031
  
  https://geojson.io/#map=9/53.8290/11.8378
  
  {
  "type": "Feature",
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [
          11.777344,
          53.644638
        ],
        [
          12.832031,
          53.644638
        ],
        [
          12.832031,
          54.265224
        ],
        [
          11.777344,
          54.265224
        ]      ]
    ]
  }
}

 */