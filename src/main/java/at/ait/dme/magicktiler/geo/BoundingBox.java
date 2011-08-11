/*
 * Copyright 2010 Austrian Institute of Technology
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package at.ait.dme.magicktiler.geo;

/**
 * A BoundingBox consisting of a north and south latitude, and
 * an east and west longitude value. 
 *
 * @author Rainer Simon <magicktiler@gmail.com>
 */
public class BoundingBox {

  private double north, south, east, west;

  public BoundingBox(double north, double south, double east, double west) {
    this.north = north;
    this.south = south;
    this.east = east;
    this.west = west;
  }

  public double getNorth() {
    return north;
  }

  public double getSouth() {
    return south;
  }

  public double getEast() {
    return east;
  }

  public double getWest() {
    return west;
  }

  public double getLatExtent() {
    return Math.abs(north - south);
  }

  public double getLonExtent() {
    return Math.abs(east - west);
  }

}
