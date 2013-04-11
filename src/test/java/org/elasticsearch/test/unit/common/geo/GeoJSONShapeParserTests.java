package org.elasticsearch.test.unit.common.geo;

import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.*;
import com.spatial4j.core.shape.impl.CircleImpl;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.spatial4j.core.shape.jts.JtsPoint;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.geo.GeoJSONShapeParser;
import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link GeoJSONShapeParser}
 */
public class GeoJSONShapeParserTests {

    private final static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Test
    public void testParse_simplePoint() throws IOException {
        String pointGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Point")
                .startArray("coordinates").value(100.0).value(0.0).endArray()
                .endObject().string();

        Point expected = GEOMETRY_FACTORY.createPoint(new Coordinate(100.0, 0.0));
        assertGeometryEquals(new JtsPoint(expected, GeoShapeConstants.SPATIAL_CONTEXT), pointGeoJson);
    }

    @Test
    public void testParse_lineString() throws IOException {
        String lineGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "LineString")
                .startArray("coordinates")
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> lineCoordinates = new ArrayList<Coordinate>();
        lineCoordinates.add(new Coordinate(100, 0));
        lineCoordinates.add(new Coordinate(101, 1));

        LineString expected = GEOMETRY_FACTORY.createLineString(
                lineCoordinates.toArray(new Coordinate[lineCoordinates.size()]));
        assertGeometryEquals(new JtsGeometry(expected, GeoShapeConstants.SPATIAL_CONTEXT, false), lineGeoJson);
    }

    @Test
    public void testParse_polygonNoHoles() throws IOException {
        String polygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Polygon")
                .startArray("coordinates")
                .startArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<Coordinate>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(
                shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        Polygon expected = GEOMETRY_FACTORY.createPolygon(shell, null);
        assertGeometryEquals(new JtsGeometry(expected, GeoShapeConstants.SPATIAL_CONTEXT, false), polygonGeoJson);
    }

    @Test
    public void testParse_polygonWithHole() throws IOException {
        String polygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Polygon")
                .startArray("coordinates")
                .startArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .endArray()
                .startArray()
                .startArray().value(100.2).value(0.2).endArray()
                .startArray().value(100.8).value(0.2).endArray()
                .startArray().value(100.8).value(0.8).endArray()
                .startArray().value(100.2).value(0.8).endArray()
                .startArray().value(100.2).value(0.2).endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<Coordinate>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        List<Coordinate> holeCoordinates = new ArrayList<Coordinate>();
        holeCoordinates.add(new Coordinate(100.2, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.2));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(
                shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        LinearRing[] holes = new LinearRing[1];
        holes[0] = GEOMETRY_FACTORY.createLinearRing(
                holeCoordinates.toArray(new Coordinate[holeCoordinates.size()]));
        Polygon expected = GEOMETRY_FACTORY.createPolygon(shell, holes);
        assertGeometryEquals(new JtsGeometry(expected, GeoShapeConstants.SPATIAL_CONTEXT, false), polygonGeoJson);
    }

    @Test
    public void testParse_multiPoint() throws IOException {
        String multiPointGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "MultiPoint")
                .startArray("coordinates")
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> multiPointCoordinates = new ArrayList<Coordinate>();
        multiPointCoordinates.add(new Coordinate(100, 0));
        multiPointCoordinates.add(new Coordinate(101, 1));

        MultiPoint expected = GEOMETRY_FACTORY.createMultiPoint(
                multiPointCoordinates.toArray(new Coordinate[multiPointCoordinates.size()]));
        assertGeometryEquals(new JtsGeometry(expected, GeoShapeConstants.SPATIAL_CONTEXT, false), multiPointGeoJson);
    }

    @Test
    public void testParse_multiPolygon() throws IOException {
        String multiPolygonGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "MultiPolygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .startArray().value(102.0).value(2.0).endArray()
                .startArray().value(103.0).value(2.0).endArray()
                .startArray().value(103.0).value(3.0).endArray()
                .startArray().value(102.0).value(3.0).endArray()
                .startArray().value(102.0).value(2.0).endArray()
                .endArray()
                .endArray()
                .startArray()
                .startArray()
                .startArray().value(100.0).value(0.0).endArray()
                .startArray().value(101.0).value(0.0).endArray()
                .startArray().value(101.0).value(1.0).endArray()
                .startArray().value(100.0).value(1.0).endArray()
                .startArray().value(100.0).value(0.0).endArray()
                .endArray()
                .startArray()
                .startArray().value(100.2).value(0.2).endArray()
                .startArray().value(100.8).value(0.2).endArray()
                .startArray().value(100.8).value(0.8).endArray()
                .startArray().value(100.2).value(0.8).endArray()
                .startArray().value(100.2).value(0.2).endArray()
                .endArray()
                .endArray()
                .endArray()
                .endObject().string();

        List<Coordinate> shellCoordinates = new ArrayList<Coordinate>();
        shellCoordinates.add(new Coordinate(100, 0));
        shellCoordinates.add(new Coordinate(101, 0));
        shellCoordinates.add(new Coordinate(101, 1));
        shellCoordinates.add(new Coordinate(100, 1));
        shellCoordinates.add(new Coordinate(100, 0));

        List<Coordinate> holeCoordinates = new ArrayList<Coordinate>();
        holeCoordinates.add(new Coordinate(100.2, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.2));
        holeCoordinates.add(new Coordinate(100.8, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.8));
        holeCoordinates.add(new Coordinate(100.2, 0.2));

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(
                shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        LinearRing[] holes = new LinearRing[1];
        holes[0] = GEOMETRY_FACTORY.createLinearRing(
                holeCoordinates.toArray(new Coordinate[holeCoordinates.size()]));
        Polygon withHoles = GEOMETRY_FACTORY.createPolygon(shell, holes);

        shellCoordinates = new ArrayList<Coordinate>();
        shellCoordinates.add(new Coordinate(102, 2));
        shellCoordinates.add(new Coordinate(103, 2));
        shellCoordinates.add(new Coordinate(103, 3));
        shellCoordinates.add(new Coordinate(102, 3));
        shellCoordinates.add(new Coordinate(102, 2));

        shell = GEOMETRY_FACTORY.createLinearRing(
                shellCoordinates.toArray(new Coordinate[shellCoordinates.size()]));
        Polygon withoutHoles = GEOMETRY_FACTORY.createPolygon(shell, null);

        MultiPolygon expected = GEOMETRY_FACTORY.createMultiPolygon(new Polygon[] {withoutHoles, withHoles});

        assertGeometryEquals(new JtsGeometry(expected, GeoShapeConstants.SPATIAL_CONTEXT, false), multiPolygonGeoJson);
    }

    @Test
    public void testThatParserExtractsCorrectTypeAndCoordinatesFromArbitraryJson() throws IOException {
        String pointGeoJson = XContentFactory.jsonBuilder().startObject()
                .startObject("crs")
                    .field("type", "name")
                    .startObject("properties")
                        .field("name", "urn:ogc:def:crs:OGC:1.3:CRS84")
                    .endObject()
                .endObject()
                .field("bbox", "foobar")
                .field("type", "point")
                .field("bubu", "foobar")
                .startArray("coordinates").value(100.0).value(0.0).endArray()
                .startObject("nested").startArray("coordinates").value(200.0).value(0.0).endArray().endObject()
                .startObject("lala").field("type", "NotAPoint").endObject()
                .endObject().string();

        Point expected = GEOMETRY_FACTORY.createPoint(new Coordinate(100.0, 0.0));
        assertGeometryEquals(new JtsPoint(expected, GeoShapeConstants.SPATIAL_CONTEXT), pointGeoJson);
    }

    @Test
    public void testThatCircleCanBeParsed() throws IOException {
        double radiusInKm = 4.2; // setting this to 2.0 gives us rounding errors... sucks

        String circleJson = XContentFactory.jsonBuilder().startObject().field("type", "Circle")
                .startArray("coordinates").value(100.0).value(10.0).endArray()
                .field("radius", radiusInKm)
                .endObject().string();

        double dist = radiusInKm / ( DistanceUtils.DEGREES_TO_RADIANS * DistanceUtils.EARTH_MEAN_RADIUS_KM);
        Circle expected = GeoShapeConstants.SPATIAL_CONTEXT.makeCircle(100, 10, dist);
        assertGeometryEquals(expected, circleJson);
    }

    // TODO: Remove me again after everything works
    @Test
    public void spatialTest() throws Exception {
        //com.spatial4j.core.shape.Point parkHotelAmsterdam = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(52.362105, 4.883208);
        //com.spatial4j.core.shape.Point schiphol = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(52.313096, 4.77253);
        com.spatial4j.core.shape.Point parkHotelAmsterdam = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(4.883208, 52.362105);
        com.spatial4j.core.shape.Point schiphol = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(4.77253, 52.313096);

        double dist15km = 10.0 / ( DistanceUtils.DEGREES_TO_RADIANS * DistanceUtils.EARTH_MEAN_RADIUS_KM);
        //com.spatial4j.core.shape.Point amsterdam = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(52.37125, 4.895439);
        com.spatial4j.core.shape.Point amsterdam = GeoShapeConstants.SPATIAL_CONTEXT.makePoint(4.895439, 52.37125);
        Circle amsterdamCityCircle = new CircleImpl(amsterdam, dist15km, GeoShapeConstants.SPATIAL_CONTEXT);

        assertThat(parkHotelAmsterdam.relate(amsterdamCityCircle).intersects(), is(true));
        assertThat(amsterdamCityCircle.relate(parkHotelAmsterdam).intersects(), is(true));
        assertThat(amsterdamCityCircle.relate(schiphol).intersects(), is(false));
        assertThat(schiphol.relate(amsterdamCityCircle).intersects(), is(false));

        double dist25km = 25.0 / ( DistanceUtils.DEGREES_TO_RADIANS * DistanceUtils.EARTH_MEAN_RADIUS_KM);
        amsterdamCityCircle = new CircleImpl(amsterdam, dist25km, GeoShapeConstants.SPATIAL_CONTEXT);

        assertThat(parkHotelAmsterdam.relate(amsterdamCityCircle).intersects(), is(true));
        assertThat(amsterdamCityCircle.relate(parkHotelAmsterdam).intersects(), is(true));
        assertThat(amsterdamCityCircle.relate(schiphol).intersects(), is(true));
        assertThat(schiphol.relate(amsterdamCityCircle).intersects(), is(true));
    }

    private void assertGeometryEquals(Shape expected, String geoJson) throws IOException {
        XContentParser parser = JsonXContent.jsonXContent.createParser(geoJson);
        parser.nextToken();
        assertEquals(GeoJSONShapeParser.parse(parser), expected);
    }
}
