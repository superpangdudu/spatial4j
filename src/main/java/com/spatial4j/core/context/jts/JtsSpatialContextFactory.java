/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spatial4j.core.context.jts;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.io.GeoJSONFormat;
import com.spatial4j.core.io.ShapeFormat;
import com.spatial4j.core.io.WKTFormat;
import com.spatial4j.core.io.jts.JtsBinaryCodec;
import com.spatial4j.core.io.jts.JtsGeoJSONFormat;
import com.spatial4j.core.io.jts.JtsWKTFormat;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;

import java.util.List;
import java.util.Map;

/**
 * See {@link SpatialContextFactory#makeSpatialContext(java.util.Map, ClassLoader)}.
 * <p/>
 * The following keys are looked up in the args map, in addition to those in the
 * superclass:
 * <DL>
 * <DT>datelineRule</DT>
 * <DD>width180(default)|ccwRect|none
 *  -- see {@link com.spatial4j.core.io.jts.JtsWKTFormat.DatelineRule}</DD>
 * <DT>validationRule</DT>
 * <DD>error(default)|none|repairConvexHull|repairBuffer0
 *  -- see {@link com.spatial4j.core.io.jts.JtsWKTFormat.ValidationRule}</DD>
 * <DT>autoIndex</DT>
 * <DD>true|false(default) -- see {@link JtsWKTFormat#isAutoIndex()}</DD>
 * <DT>allowMultiOverlap</DT>
 * <DD>true|false(default) -- see {@link JtsSpatialContext#isAllowMultiOverlap()}</DD>
 * <DT>precisionModel</DT>
 * <DD>floating(default) | floating_single | fixed
 *  -- see {@link com.vividsolutions.jts.geom.PrecisionModel}.
 * If {@code fixed} then you must also provide {@code precisionScale}
 *  -- see {@link com.vividsolutions.jts.geom.PrecisionModel#getScale()}</DD>
 * </DL>
 */
public class JtsSpatialContextFactory extends SpatialContextFactory {

  protected static final PrecisionModel defaultPrecisionModel = new PrecisionModel();//floating

  //These 3 are JTS defaults for new GeometryFactory()
  public PrecisionModel precisionModel = defaultPrecisionModel;
  public int srid = 0;
  public CoordinateSequenceFactory coordinateSequenceFactory = CoordinateArraySequenceFactory.instance();

  //ignored if geo=false
  public JtsWKTFormat.DatelineRule datelineRule = JtsWKTFormat.DatelineRule.width180;

  public JtsWKTFormat.ValidationRule validationRule = JtsWKTFormat.ValidationRule.error;
  public boolean autoIndex = false;
  public boolean allowMultiOverlap = false;//ignored if geo=false

  //kinda advanced options:
  public boolean useJtsPoint = true;
  public boolean useJtsLineString = true;

  public JtsSpatialContextFactory() {
    super.binaryCodecClass = JtsBinaryCodec.class;
  }
  
  @Override
  protected void init(Map<String, String> args, ClassLoader classLoader) {
    super.init(args, classLoader);

    initField("datelineRule");
    initField("validationRule");
    initField("autoIndex");
    initField("allowMultiOverlap");
    initField("useJtsPoint");
    initField("useJtsLineString");

    String scaleStr = args.get("precisionScale");
    String modelStr = args.get("precisionModel");

    if (scaleStr != null) {
      if (modelStr != null && !modelStr.equals("fixed"))
        throw new RuntimeException("Since precisionScale was specified; precisionModel must be 'fixed' but got: "+modelStr);
      precisionModel = new PrecisionModel(Double.parseDouble(scaleStr));
    } else if (modelStr != null) {
      if (modelStr.equals("floating")) {
        precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
      } else if (modelStr.equals("floating_single")) {
        precisionModel = new PrecisionModel(PrecisionModel.FLOATING_SINGLE);
      } else if (modelStr.equals("fixed")) {
        throw new RuntimeException("For fixed model, must specifiy 'precisionScale'");
      } else {
        throw new RuntimeException("Unknown precisionModel: "+modelStr);
      }
    }
  }

  @Override
  protected void verifySupportedFormats(List<ShapeFormat> registry, SpatialContext ctx) {
    boolean hasWKT = false;
    boolean hasGeoJSON = false;
    for(ShapeFormat fmt : registry) {
      if(GeoJSONFormat.FORMAT.equals(fmt.getFormatName())) {
        hasGeoJSON = true;
      }
      else if(WKTFormat.FORMAT.equals(fmt.getFormatName())) {
        hasWKT = true;
      }
    }
    if(!hasGeoJSON) {
      registry.add(new JtsGeoJSONFormat((JtsSpatialContext)ctx, this));
    }
    if(!hasWKT) {
      registry.add(new JtsWKTFormat((JtsSpatialContext)ctx, this));
    }
  }
  
  public GeometryFactory getGeometryFactory() {
    if (precisionModel == null || coordinateSequenceFactory == null)
      throw new IllegalStateException("precision model or coord seq factory can't be null");
    return new GeometryFactory(precisionModel, srid, coordinateSequenceFactory);
  }

  @Override
  public JtsSpatialContext newSpatialContext() {
    return new JtsSpatialContext(this);
  }
}
