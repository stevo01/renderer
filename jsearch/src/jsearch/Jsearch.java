/* Copyright 2014 Malcolm Herring
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * For a copy of the GNU General Public License, see <http://www.gnu.org/licenses/>.
 */

package jsearch;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.*;


public class Jsearch {
	
	private static final Logger log = Logger.getLogger( Jsearch.class.getName() );
	
	public static void main(String[] args) throws Exception 
	{
		log.setLevel(Level.INFO);
		log.info("bgn: Jsearch");
		long startTime = System.currentTimeMillis();
		
		String dir = args[0];
		
		if(args.length == 1)
		{			
			generate_diff(dir);
		}
		else if(args.length == 3)
		{
			int x_z9 = Integer.parseInt(args[1]);
			int y_z9 = Integer.parseInt(args[2]);
			bbinfo(x_z9, y_z9);
			generate_static(dir, x_z9, y_z9);
		}	
		else if(args.length == 3)
		{
			System.err.println("Usage: java -jar jsearch.jar <osm directory>");
			System.err.println("");
			System.err.println("optional usage: java -jar jsearch.jar <osm directory> <xtile z=9> <ytile z=9>");
            System.exit(0);
		}
		
		
		log.info("end: Jsearch\n");

		long stopTime = System.currentTimeMillis();
		log.info(String.format("Runtime = %d ms",stopTime - startTime));
		
	}
	
	static void bbinfo(int x_z9, int y_z9) 
	{
		MapBB bb = new MapBB();
		bb.minlon = tile2lon(x_z9, 9);
		bb.maxlon = tile2lon(x_z9+1, 9);
		bb.minlat = tile2lat(y_z9, 9);
		bb.maxlat = tile2lat(y_z9+1, 9);
		
		MapBB bb_ext = new MapBB();
		int x = x_z9 * 8;
		int y = y_z9 * 8;
		bb_ext.minlon = tile2lon((x + 4094) % 4096, 12);
		bb_ext.maxlon = tile2lon((x + 10) % 4095, 12);
		bb_ext.minlat = tile2lat(Math.min((y + 10), 4095), 12);
		bb_ext.maxlat = tile2lat(Math.max((y - 2), 0), 12);
		
		log.info("bb     info" + bb.getinfo());
		log.info("bb_ext info" + bb_ext.getinfo());
		
	}
	
	// select all tiles for a given area ( zoom level 9 - 12 )
	static void generate_static(String dir, int x_z9, int y_z9) throws Exception
    {
		HashMap<Integer, Boolean> z9s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z10s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z11s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z12s = new HashMap<Integer, Boolean>();
		
		z9s.put((x_z9 * 512) + (y_z9), true);
		
		for(int x = 0; x<2; x++ ) 
		{
			for(int y = 0; y<2; y++ ) 
			{
				z10s.put(((x_z9*2+x) * 1024) + (y_z9*2+y), true);
			}
		}
		
		for(int x = 0; x<4; x++ ) 
		{
			for(int y = 0; y<4; y++ ) 
			{
				z11s.put(((x_z9*4+x) * 2048) + (y_z9*4+y), true);
			}
		}
		
		for(int x = 0; x<8; x++ ) 
		{
			for(int y = 0; y<8; y++ ) 
			{
				z12s.put(((x_z9*8+x) * 4096) + (y_z9*8+y), true);
			}
		}
	
		generate_osm_extract(dir, z9s, z10s, z11s, z12s);
		return;
		
    }

	static void generate_diff(String dir) throws Exception
    {
		HashMap<Long, Boolean> cnodes = new HashMap<Long, Boolean>();
		HashMap<Long, Boolean> cways = new HashMap<Long, Boolean>();
		HashMap<Long, Boolean> crels = new HashMap<Long, Boolean>();
		HashMap<Long, Boolean> nnodes = new HashMap<Long, Boolean>();
		HashMap<Long, Boolean> nways = new HashMap<Long, Boolean>();
		HashMap<Long, Boolean> nrels = new HashMap<Long, Boolean>();

		HashMap<Integer, Boolean> z9s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z10s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z11s = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> z12s = new HashMap<Integer, Boolean>();
		
		long id = 0;
		
		String InputFile=dir+"diffs";
		
		log.info("Start of Jsearch\n");
		log.info("analyse diff file="+InputFile+"\n");
		
		BufferedReader in = new BufferedReader(new FileReader(dir + "diffs"));
		String ln;
		while ((ln = in.readLine()) != null) 
		{
			for (String token : ln.split("[ ]+")) 
			{
				if (token.matches("^id=.+")) 
				{
					id = Long.parseLong(token.split("[\"\']")[1]);
					break;
				}
			}
			if (ln.matches("^<.+")) 
			{
				if (ln.contains("<node")) 
				{
					cnodes.put(id, true);
				} else if (ln.contains("<way")) 
				{
					cways.put(id, true);
				} else if (ln.contains("<relation")) 
				{
					crels.put(id, true);
				}
			} 
			else if (ln.matches("^>.+")) 
			{
				if (ln.contains("<node")) 
				{
					nnodes.put(id, true);
				} else if (ln.contains("<way")) 
				{
					nways.put(id, true);
				} else if (ln.contains("<relation")) 
				{
					nrels.put(id, true);
				}
			}
		}
		in.close();
		
		
		log.info(String.format("new nodes=%d%n",nnodes.size()));
		log.info(String.format("new  ways=%d%n",nways.size()));
		log.info(String.format("new  rels=%d%n",nrels.size()));
		
		log.info(String.format("removed nodes=%d%n",cnodes.size()));
		log.info(String.format("removed  ways=%d%n",cways.size()));
		log.info(String.format("removed  rels=%d%n",crels.size()));
		
		boolean next = false;
		do 
		{
			if (next) {
				in = new BufferedReader(new FileReader(dir + "next.osm"));
				log.info("analyse file next.osm");
			} else {
				in = new BufferedReader(new FileReader(dir + "world.osm"));
				log.info("analyse file world.osm");
			}
			
			boolean inOsm = false;
			boolean inNode = false;
			boolean inWay = false;
			boolean inRel = false;
			ArrayList<String> buf = new ArrayList<String>();
			
			log.info("start: copy content from file to ram buffer\n");
			
			while ((ln = in.readLine()) != null) 
			{
				if (inOsm) 
				{
					if (inNode) 
					{
						if (ln.contains("</node")) {
							buf.add(ln);
							inNode = false;
						}
					} 
					else if (ln.contains("<node")) 
					{
						buf.add(ln);
						if (!ln.contains("/>")) {
							inNode = true;
						}
					} 
					else if (inWay) 
					{
						if (ln.contains("<nd")) {
							buf.add(ln);
						}
						if (ln.contains("</way")) {
							buf.add(ln);
							inWay = false;
						}
					} 
					else if (ln.contains("<way")) 
					{
						buf.add(ln);
						if (!ln.contains("/>")) {
							inWay = true;
						}
					} 
					else if (inRel) 
					{
						if (ln.contains("<member")) 
						{
							String type = "";
							String role = "";
							long ref = 0;
							for (String token : ln.split("[ ]+")) 
							{
								if (token.matches("^ref=.+")) {
									ref = Long.parseLong(token.split("[\"\']")[1]);
								} else if (token.matches("^type=.+")) {
									type = (token.split("[\"\']")[1]);
								} else if (token.matches("^role=.+")) {
									role = (token.split("[\"\']")[1]);
								}
							}
							if ((role.equals("outer") || role.equals("inner")) && type.equals("way")) 
							{
								if (next) {
									nways.put(ref, true);
								} else {
									cways.put(ref, true);
								}
							}
						}
						if (ln.contains("</relation")) {
							inRel = false;
						}
					} 
					else if (ln.contains("<relation")) 
					{
						for (String token : ln.split("[ ]+")) 
						{
							if (token.matches("^id=.+")) {
								id = Long.parseLong(token.split("[\"\']")[1]);
							}
						}
						if (((next && nrels.containsKey(id)) || (!next && crels.containsKey(id))) && !ln.contains("/>")) {
							inRel = true;
						}
					} 
					else if (ln.contains("</osm")) 
					{
						log.info("end of osm section found");
						buf.add(ln);
						inOsm = false;
						break;
					}
				} 
				else if (ln.contains("<osm")) 
				{
					log.info("start of osm section found");
					buf.add(ln);
					inOsm = true;
				}
			}
			in.close();
			
			log.info("end: copy content from file to ram buffer");
			
			log.info("start: process way(s)");
			inOsm = false;
			inWay = false;
			for (String line : buf) 
			{
				ln = line;
				if (inOsm) 
				{
					if (inWay) 
					{
						if (ln.contains("<nd")) 
						{
							for (String token : ln.split("[ ]+")) 
							{
								if (token.matches("^ref=.+")) {
									id = Long.parseLong(token.split("[\"\']")[1]);
								}
							}
							if (next) {
								nnodes.put(id, true);
							} else {
								cnodes.put(id, true);
							}
						}
						if (ln.contains("</way")) {
							inWay = false;
						}
					} 
					else if (ln.contains("<way")) 
					{
						for (String token : ln.split("[ ]+")) 
						{
							if (token.matches("^id=.+")) {
								id = Long.parseLong(token.split("[\"\']")[1]);
							}
						}
						if (((next && nways.containsKey(id)) || (!next && cways.containsKey(id))) && !ln.contains("/>")) {
							inWay = true;
						}
					} else if (ln.contains("</osm")) {
						inOsm = false;
						break;
					}
				} else if (ln.contains("<osm")) {
					inOsm = true;
				}
			}
			
			log.info("end: process ways");
			
			log.info("start: process node");
			for (String line : buf) 
			{
				ln = line;
				if (ln.contains("<node")) 
				{
					Double lat = 0.0;
					Double lon = 0.0;
					for (String token : ln.split("[ ]+")) 
					{
						if (token.matches("^id=.+")) 
						{
							id = Long.parseLong(token.split("[\"\']")[1]);
						} 
						else if (token.matches("^lat=.+")) 
						{
							lat = Double.parseDouble(token.split("[\"\']")[1]);
						} 
						else if (token.matches("^lon=.+")) 
						{
							lon = Double.parseDouble(token.split("[\"\']")[1]);
						}
					}
					if ((next && nnodes.containsKey(id)) || (!next && cnodes.containsKey(id))) 
					{
						int xtile = lon2xtile(lon, 12);
						int ytile = lat2ytile(lat, 12);
						z9s.put(((xtile / 8) * 512) + (ytile / 8), true);
						z10s.put(((xtile / 4) * 1024) + (ytile / 4), true);
						z11s.put(((xtile / 2) * 2048) + (ytile / 2), true);
						for (int x = xtile - 1; x <= xtile + 1; x++) 
						{
							for (int y = ytile - 1; y <= ytile + 1; y++) 
							{
								if ((y >= 0) && (y <= 4095)) 
								{
									z12s.put((((x < 0) ? 4095 : (x > 4095) ? 0 : x) * 4096) + y, true);
								}
							}
						}
					}
				}
			}
			log.info("end: process node");
			
			next = !next;
		} while (next);
		
		generate_osm_extract(dir, z9s, z10s, z11s, z12s);
		return;
	}
		
	// generate the osm files for zoom level 9-12
	static void generate_osm_extract(String dir,
									 HashMap<Integer, Boolean> z9s, 
				  					 HashMap<Integer, Boolean> z10s,
				 					 HashMap<Integer, Boolean> z11s,
									 HashMap<Integer, Boolean> z12s) throws Exception
	{			
		
		log.info("bgn: generate osm files");
		log.info(String.format("number of files  z9s=%d", z9s.size()));
		log.info(String.format("number of files z10s=%d", z10s.size()));
		log.info(String.format("number of files z11s=%d", z11s.size()));
		log.info(String.format("number of files z12s=%d", z12s.size()));
				
		log.info("bgn: process z9s");
		for (int t : z9s.keySet()) 
		{
			String z9nam=dir + "tmp/" + (t / 512) + "-" + (t % 512) + "-9.osm";
			log.info("generate file"+z9nam);
				
		    // determine x.y tile from zoom level 12
			int x = (t / 512) * 8;
			int y = (t % 512) * 8;
			MapBB bb = new MapBB();
			bb.minlon = tile2lon((x + 4094) % 4096, 12);
			bb.maxlon = tile2lon((x + 10) % 4095, 12);
			bb.minlat = tile2lat(Math.min((y + 10), 4095), 12);
			bb.maxlat = tile2lat(Math.max((y - 2), 0), 12);
			
			// todo: the call of following time is time consuming
			//       parse osm file (with seamark extract of planet)
			//       evaluate usage of 
			//       a) database with gis extensions
			//       b) (lib)osmium 
			log.info("bounding box:" + bb.getstring());
			log.info("bounding box:" + bb.geojson());
			ArrayList<String> ext = Extract.extractData(dir + "next.osm", bb);
			
			if(ext.size() > 4) 
			{
				PrintStream out = new PrintStream(z9nam);
				for (String line : ext) {
					out.println(line);
				}
				out.close();
			}
			else
			{
				log.info("skip generation of file "+z9nam);
				continue;
			}
		}
		log.info("end: process z9s\n");
		
		log.info("start: process z10s\n");
		for (int t : z10s.keySet()) 
		{
			String z9nam=dir + "tmp/" + ((t / 1024) / 2) + "-" + ((t % 1024) / 2) + "-9.osm";
			log.info("parse file "+z9nam);
			
			String z10nam=dir + "tmp/" + (t / 1024) + "-" + (t % 1024) + "-10.osm";
			log.info("generate file "+z10nam);
			
			int x = (t / 1024) * 4;
			int y = (t % 1024) * 4;
			MapBB bb = new MapBB();
			
			// note: calculate the bounding box which is bigger then the tile area
			bb.minlon = tile2lon((x + 4094) % 4096, 12);
			bb.maxlon = tile2lon((x + 6) % 4095, 12);
			bb.minlat = tile2lat(Math.min((y + 6), 4095), 12);
			bb.maxlat = tile2lat(Math.max((y - 2), 0), 12);
			log.info("bounding box:" + bb.geojson());
			try{
				ArrayList<String> ext = Extract.extractData(z9nam, bb);
				if(ext.size() > 4) {
					PrintStream out = new PrintStream(z10nam);
					for (String line : ext) {
						out.println(line);
					}
					out.close();
				}
				else
				{
					log.info("skip generation of file "+z10nam);
					continue;	
				}
			}
			catch (IOException ex)
			{
				log.info("skip generation of file "+z10nam);
				continue;
			}
		}
		log.info("end: process z10s\n");
			
		log.info("start: process z11s\n");
		for (int t : z11s.keySet()) 
		{
			String z10nam=dir + "tmp/" + ((t / 2048) / 2) + "-" + ((t % 2048) / 2) + "-10.osm";
			log.info("parse file "+z10nam);
			
			String z11nam = dir + "tmp/" + (t / 2048) + "-" + (t % 2048) + "-11.osm";
			log.info("generate file "+z11nam);
			
			int x = (t / 2048) * 2;
			int y = (t % 2048) * 2;
			MapBB bb = new MapBB();
			bb.minlon = tile2lon((x + 4094) % 4096, 12);
			bb.maxlon = tile2lon((x + 4) % 4095, 12);
			bb.minlat = tile2lat(Math.min((y + 4), 4095), 12);
			bb.maxlat = tile2lat(Math.max((y - 2), 0), 12);
			log.info("bounding box:" + bb.geojson());
			try
			{
				ArrayList<String> ext = Extract.extractData(z10nam, bb);
				if(ext.size() > 4) 
				{
					PrintStream out = new PrintStream(z11nam);
					for (String line : ext) {
						out.println(line);
					}
					out.close();
				}
				else
				{
					log.info("skip generation of file "+z11nam);
					continue;
				}
				
				
				for (int i = (x+4095)%4096; i < x+3; i = (i+1)%4096) 
				{
					for (int j = Math.max(y-1, 0); j < y+3; j = Math.min(j+1, 4095)) 
					{
						if (z12s.containsKey(i*4096+j)) 
						{
	
							log.info("parse file "+z11nam);
							
							String z12nam = dir + "tmp/" + i + "-" + j + "-12.osm";
							log.info("generate file "+z12nam);
							
							z12s.remove(i*4096+j);
							bb = new MapBB();
							bb.minlon = tile2lon((i + 4095) % 4096, 12);
							bb.maxlon = tile2lon((i + 2) % 4095, 12);
							bb.minlat = tile2lat(Math.min((j + 2), 4095), 12);
							bb.maxlat = tile2lat(Math.max((j - 1), 0), 12);
							log.info("bounding box:" + bb.geojson());
							ext = Extract.extractData(z11nam, bb);
							if(ext.size() > 4) 
							{
								PrintStream out = new PrintStream(z12nam);
								for (String line : ext) {
									out.println(line);
								}
								out.close();
							}
							else
							{
								log.info("skip generation of file "+z12nam);
								continue;
							}
						}
					}
				}
			}
			catch (IOException ex)
			{
				log.info("skip generation of file "+z11nam);
				continue;
			}
		}
	}

	static int lon2xtile(double lon, int zoom) 
	{
		int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		return (xtile);
	}

	static int lat2ytile(double lat, int zoom) 
	{
		int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		return (ytile);
	}

	MapBB tile2bb(final int x, final int y, final int zoom) 
	{
		MapBB bb = new MapBB();
		bb.maxlat = tile2lat(y, zoom);
		bb.minlat = tile2lat(y + 1, zoom);
		bb.minlon = tile2lon(x, zoom);
		bb.maxlon = tile2lon(x + 1, zoom);
		return bb;
	}

	static double tile2lon(int x, int z) 
	{
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	static double tile2lat(int y, int z) 
	{
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
}
