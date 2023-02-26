package net.calm.iaclasslibrary.IAClasses;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Straightener;
import ij.plugin.filter.EDM;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author barry05
 */
public class Region {

    protected ArrayList<short[]> seedPix = new ArrayList<short[]>();
    protected ArrayList<Pixel> pix = new ArrayList<Pixel>();
    protected LinkedList<short[]> borderPix = new LinkedList<short[]>();
    protected LinkedList<short[]> expandedBorder = new LinkedList<short[]>();
    protected ArrayList<float[]> centres = new ArrayList<float[]>();
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE, mean, seedMean, sigma;
    private double mfD[];
    protected boolean edge, active;
    protected Rectangle bounds;
    private int[] histogram = new int[256];
    public final static short MASK_FOREGROUND = 0, MASK_BACKGROUND = 255;
    protected int imageWidth, imageHeight;
    private ImageProcessor mask;
    private int index;
    private ArrayList<Double> morphMeasures;

    public Region() {

    }

    public Region(int index, int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.index = index;
    }

    public Region(ImageProcessor mask, double[] centre) {
        this(mask, new short[]{(short) Math.round(centre[0]), (short) Math.round(centre[1])});
    }

    public Region(ImageProcessor mask, float[] centre) {
        this(mask, new double[]{centre[0], centre[1]});
    }

    /**
     * Creates a new Region instance.
     * 
     * @param mask A binary mask image that defines the region
     * @param centre The centre of the mask - any point within the mask object 
     * in the form [x, y].
     */
    public Region(ImageProcessor mask, short[] centre) {
        this.active = true;
        this.imageWidth = mask.getWidth();
        this.imageHeight = mask.getHeight();
        if (centre != null) {
            this.centres.add(new float[]{centre[0], centre[1]});
        }
        this.mask = mask;
        this.newBounds(centre);
        short[][] bp = this.getOrderedBoundary(imageWidth, imageHeight, mask, centre);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                this.addBorderPoint(bp[i], mask);
            }
        }
        drawMask(imageWidth, imageHeight);
    }

    public Region(int width, int height, short[] centre) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.centres.add(new float[]{centre[0], centre[1]});
        this.mask = new ByteProcessor(width, height);
        this.mask.setValue(MASK_BACKGROUND);
        this.mask.fill();
        this.newBounds(centre);
        this.addBorderPoint(centre, mask);
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

    void newBounds(short[] point) {
        if (point != null) {
            bounds = new Rectangle(point[0], point[1], 1, 1);
        }
    }

    public final void addBorderPoint(short[] point, ImageProcessor mask) {
        borderPix.add(point);
        updateBounds(point);
        mask.drawPixel(point[0], point[1]);
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        Arrays.fill(histogram, 0);
        int width = mask.getWidth();
        int height = mask.getHeight();
        int size = mask.getStatistics().histogram[MASK_FOREGROUND];
        double valSum = 0.0, varSum = 0.0, pix;
        if (size > 0) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (mask.getPixel(i, j) == MASK_FOREGROUND) {
                        pix = refImage.getPixelValue(i, j);
                        if (pix < min) {
                            min = pix;
                        } else if (pix > max) {
                            max = pix;
                        }
                        valSum += pix;
                        int bin = (int) Math.floor(pix);
                        histogram[bin]++;
                    }
                }
            }
            mean = valSum / (size);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (mask.getPixel(i, j) == MASK_FOREGROUND) {
                        varSum += Math.pow(mean - refImage.getPixelValue(i, j), 2);
                    }
                }
            }
            sigma = Math.sqrt(varSum) / size;
        }
    }

    public void calcCentroid(ImageProcessor mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        int count = 0;
        float xsum = 0.0f, ysum = 0.0f;
        byte[] pix = (byte[]) mask.getPixels();
        for (int j = 0; j < height; j++) {
            int offset = j * width;
            for (int i = 0; i < width; i++) {
                if (pix[i + offset] != (byte) Region.MASK_BACKGROUND) {
                    xsum += i;
                    ysum += j;
                    count++;
                }
            }
        }
//        double x = xsum / count;
//        double y = ysum / count;
//        if (mask.getPixel((int) net.calm.iaclasslibrary.Math.round(x), (int) net.calm.iaclasslibrary.Math.round(y)) < Region.BACKGROUND) {
        centres.add(new float[]{xsum / count, ysum / count});
//            return true;
//        } else {
//            return false;
//        }
    }

    public PolygonRoi getPolygonRoi(ImageProcessor mask) {
//        int bordersize = borderPix.size();
//        mask.setRoi(r);
//        mask = mask.crop();
//        int rx = 0, ry = 0;
//        if (r != null) {
//            rx = r.x;
//            ry = r.y;
//        }
//        tempImage.setValue(BACKGROUND);
//        tempImage.fill();
//        tempImage.setValue(FOREGROUND);
//        Polygon poly = new Polygon();
//        for (int i = 0; i < bordersize; i++) {
//            tempImage.drawPixel(borderPix.get(i).getX() - r.x, borderPix.get(i).getY() - r.y);
////            poly.addPoint(borderPix.get(i).getX(), borderPix.get(i).getY());
//        }
//        fill(tempImage, FOREGROUND, BACKGROUND);
        ArrayList<float[]> centres = getCentres();
        float[] centre = centres.get(centres.size() - 1);
//        short xc = (short) net.calm.iaclasslibrary.Math.round(centre[0] - rx);
//        short yc = (short) net.calm.iaclasslibrary.Math.round(centre[1] - ry);
        short xc = (short) Math.round(centre[0]);
        short yc = (short) Math.round(centre[1]);
        int[][] pix = getMaskOutline(new short[]{xc, yc}, mask);
        if (pix != null) {
            return new PolygonRoi(pix[0], pix[1], pix[0].length, Roi.POLYGON);
        } else {
            return null;
        }
    }

    /**
     * net.calm.iaclasslibrary.Binary fill by Gabriel Landini, G.Landini at bham.ac.uk. Copied from
     * ij.plugin.filter.net.calm.iaclasslibrary.Binary
     *
     * @param ip
     * @param foreground
     * @param background
     */
    void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y = 0; y < height; y++) {
            if (ip.getPixel(0, y) == background) {
                ff.fill(0, y);
            }
            if (ip.getPixel(width - 1, y) == background) {
                ff.fill(width - 1, y);
            }
        }
        for (int x = 0; x < width; x++) {
            if (ip.getPixel(x, 0) == background) {
                ff.fill(x, 0);
            }
            if (ip.getPixel(x, height - 1) == background) {
                ff.fill(x, height - 1);
            }
        }
        byte[] pixels = (byte[]) ip.getPixels();
        int n = width * height;
        for (int i = 0; i < n; i++) {
            if (pixels[i] == 127) {
                pixels[i] = (byte) background;
            } else {
                pixels[i] = (byte) foreground;
            }
        }
        ip.setColor(foreground);
    }

    public double getMean() {
        return mean;
    }

    public double getSigma() {
        return sigma;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

//    public ArrayList<Pixel> getPixels() {
//        return pixels;
//    }
    public int borderContains(int x, int y) {
        int i;
        for (i = 0; i < borderPix.size(); i++) {
            short[] p = borderPix.get(i);
            if ((p[0] == x) && (p[1] == y)) {
                return i;
            }
        }
        return -1;
    }

    public LinkedList<short[]> getBorderPix() {
        return borderPix;
    }

    public void removeBorderPoint(int index) {
        borderPix.remove(index);
    }

    public boolean expandBorder() {
        if (expandedBorder.size() < 1) {
            return false;
        } else {
            borderPix = expandedBorder;
            expandedBorder = new LinkedList<short[]>();
            return true;
        }
    }

    public void addExpandedBorderPix(short[] p) {
        expandedBorder.add(p);
        updateBounds(p);
        mask.drawPixel(p[0], p[1]);
    }

    public LinkedList<short[]> getExpandedBorder() {
        return expandedBorder;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Region other = (Region) obj;
        if (Double.doubleToLongBits(this.min) != Double.doubleToLongBits(other.min)) {
            return false;
        }
        if (Double.doubleToLongBits(this.max) != Double.doubleToLongBits(other.max)) {
            return false;
        }
        if (Double.doubleToLongBits(this.mean) != Double.doubleToLongBits(other.mean)) {
            return false;
        }
        if (Double.doubleToLongBits(this.sigma) != Double.doubleToLongBits(other.sigma)) {
            return false;
        }
        return true;
    }

    public boolean isEdge() {
        return edge;
    }

    public void setEdge(boolean edge) {
        this.edge = edge;
    }

    public ArrayList<short[]> getSeedPix() {
        return seedPix;
    }

    public ArrayList<short[]> getPixels() {
        Rectangle bounds = getBounds();
        ImageProcessor mask = getMask();
        ArrayList<short[]> pix = new ArrayList<>();
        for (int y = bounds.y; y < bounds.height + bounds.y; y++) {
            for (int x = bounds.x; x < bounds.width + bounds.x; x++) {
                if (mask.getPixel(x, y) == Region.MASK_FOREGROUND) {
                    pix.add(new short[]{(short) x, (short) y, 0, 1});
                }
            }
        }
        return pix;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    void updateBounds(short[] pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel[0];
        int y = pixel[1];
        if (bounds == null) {
            bounds = new Rectangle(x, y, 1, 1);
            return;
        }
        if (x < bounds.x) {
            int inc = bounds.x - x;
            bounds.x -= inc;
            bounds.width += inc;
        } else if (x > bounds.x + bounds.width) {
            bounds.width = x - bounds.x;
        }
        if (y < bounds.y) {
            int inc = bounds.y - y;
            bounds.y -= inc;
            bounds.height += inc;
        } else if (y > bounds.y + bounds.height) {
            bounds.height = y - bounds.y;
        }
    }

    public double[][] getDataArray(ImageProcessor refImage) {
        double data[][] = new double[bounds.width + 1][bounds.height + 1];
        for (int i = 0; i < bounds.width; i++) {
            Arrays.fill(data[i], Double.NaN);
        }
        for (int i = 0; i < bounds.width; i++) {
            for (int j = 0; j < bounds.height; j++) {
                data[i][j] = refImage.getPixelValue(i, j);
            }
        }
        return data;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public double[] getMfD() {
        return mfD;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getSeedMean() {
        return seedMean;
    }

    void drawMask(int width, int height) {
        ImageProcessor mask = new ByteProcessor(width, height);
        mask.setColor(MASK_BACKGROUND);
        mask.fill();
        mask.setColor(MASK_FOREGROUND);
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            short[] current = borderPix.get(i);
            mask.drawPixel(current[0], current[1]);
        }
        fill(mask, MASK_FOREGROUND, MASK_BACKGROUND);
        this.mask = mask;
    }

    public ImageProcessor getMask() {
        if (mask == null) {
            drawMask(imageWidth, imageHeight);
        } else if (mask.getWidth() != imageWidth || mask.getHeight() != imageHeight) {
            return constructFullSizeMask(imageWidth, imageHeight);
        }
        return mask.duplicate();
    }

    public short[][] getOrderedBoundary(int width, int height, ImageProcessor mask, float[] centre) {
        return getOrderedBoundary(width, height, mask, new short[]{(short) Math.round(centre[0]), (short) Math.round(centre[1])});
    }

    public short[][] getOrderedBoundary(int width, int height, ImageProcessor mask, short[] centre) {
        int[][] pix = getMaskOutline(centre, mask);
        if (pix == null) {
            return null;
        } else {
            return DSPProcessor.interpolatePoints(pix[0].length, pix[0], pix[1]);
        }
    }

    int[][] getMaskOutline(short[] centre, ImageProcessor mask) {
        if (centre == null || mask.getPixel(centre[0], centre[1]) != MASK_FOREGROUND) {
            short[] seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        Wand wand = new Wand(mask);
        wand.autoOutline(centre[0], centre[1], 0.0, Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpix = new int[n];
        int[] ypix = new int[n];
        System.arraycopy(wand.xpoints, 0, xpix, 0, wand.npoints);
        System.arraycopy(wand.ypoints, 0, ypix, 0, wand.npoints);
        for (int j = 0; j < n; j++) {
            xpix[j] = wand.xpoints[j];
            ypix[j] = wand.ypoints[j];
            if (xpix[j] >= imageWidth) {
                xpix[j] = imageWidth - 1;
            }
            if (ypix[j] >= imageHeight) {
                ypix[j] = imageHeight - 1;
            }
        }
        return new int[][]{xpix, ypix};
    }

    public float[][] buildVelMapCol(short xc, short yc, ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        drawMask(ip.getWidth(), ip.getHeight());
        short points[][] = getOrderedBoundary(ip.getWidth(), ip.getHeight(),
                mask, new short[]{xc, yc});
        float floatPoints[][] = new float[points.length][3];
        double t1 = 0, t2 = 0;
        if (frame > 1 && frame < size) {
            ipm1 = stack.getProcessor(frame - 1);
            ipp1 = stack.getProcessor(frame + 1);
        }
        if (frame > 1 && frame < size) {
            t1 = thresholds[frame - 2];
            t2 = thresholds[frame];
        }
        for (int i = 0; i < points.length; i++) {
            int x = points[i][0];
            int y = points[i][1];
            double g = 0.0;
            if (frame > 1 && frame < size) {
                g = ipp1.getPixelValue(x, y) - t1 - ipm1.getPixelValue(x, y) + t2;
            }
            double delta = edges.getPixelValue(x, y);
            double z;
            if (delta == 0.0) {
                z = 0.0;
            } else {
                z = g / delta;
            }
            floatPoints[i] = new float[]{x, y, (float) (z * spatialRes * timeRes)};
        }
        return floatPoints;
    }

    public ImageProcessor buildVelImage(ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloatProcessor output = new FloatProcessor(width, height);
        float outPix[] = (float[]) output.getPixels();
        short ipm1[] = null, ipp1[] = null;
        FloatProcessor edges = (FloatProcessor) (new TypeConverter(ip.duplicate(), false)).convertToFloat(null);
        edges.findEdges();
        float edgePix[] = (float[]) edges.getPixels();
        int size = stack.getSize();
        float t1 = 0, t2 = 0;
        float spatTimeRes = (float) (spatialRes * timeRes);
        if (frame > 1 && frame < size) {
            ipm1 = (short[]) ((new TypeConverter(stack.getProcessor(frame - 1), false)).convertToShort()).getPixels();
            ipp1 = (short[]) ((new TypeConverter(stack.getProcessor(frame + 1), false)).convertToShort()).getPixels();
        }
        if (frame > 1 && frame < size) {
            t1 = (float) (thresholds[frame - 2]);
            t2 = (float) (thresholds[frame]);
        }
        for (int j = 0; j < height; j++) {
            int offset = j * width;
            for (int i = 0; i < width; i++) {
                float g = 0;
                if (frame > 1 && frame < size) {
                    g = ipp1[i + offset] - t1 - ipm1[i + offset] + t2;
                }
                float delta = edgePix[i + offset];
                float z;
                if (delta == 0.0) {
                    z = 0.0f;
                } else {
                    z = g / delta;
                }
                outPix[i + offset] = z * spatTimeRes;
            }
        }
        output.setPixels(outPix);
        return output;
    }
    
    public float[][] buildMapCol(ImageProcessor ip, int finalWidth, int depth) {
        if (depth < 3) {
            depth = 3;
        }
        PolygonRoi proi = getPolygonRoi(getMask());
        if (proi == null) {
            return null;
        }
        Straightener straightener = new Straightener();
        ImagePlus sigImp = new ImagePlus("", ip);
        sigImp.setRoi(proi);
        ImageProcessor sig = straightener.straighten(sigImp, proi, depth);
        FloatPolygon fPoly = proi.getFloatPolygon();
        FloatProcessor xp = new FloatProcessor(fPoly.npoints, depth);
        FloatProcessor yp = new FloatProcessor(fPoly.npoints, depth);
        for (int x = 0; x < fPoly.npoints; x++) {
            for (int y = 0; y < depth; y++) {
                xp.putPixelValue(x, y, fPoly.xpoints[x]);
                yp.putPixelValue(x, y, fPoly.ypoints[x]);
            }
        }
        xp.setInterpolate(true);
        xp.setInterpolationMethod(ImageProcessor.BILINEAR);
        yp.setInterpolate(true);
        yp.setInterpolationMethod(ImageProcessor.BILINEAR);
        sig.setInterpolate(true);
        sig.setInterpolationMethod(ImageProcessor.BILINEAR);
        ImageProcessor sig2 = sig.resize(finalWidth, depth);
        ImageProcessor xp2 = xp.resize(finalWidth, depth);
        ImageProcessor yp2 = yp.resize(finalWidth, depth);
        float points[][] = new float[finalWidth][3];
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = 0; y < sig2.getHeight(); y++) {
                sum += sig2.getPixelValue(x, y);
            }
            points[x] = new float[]{(float) xp2.getPixelValue(x, 0), (float) yp2.getPixelValue(x, 0), (float) (sum / depth)};
        }
        return points;
    }
    
    public float[][] buildMapCol(ImageProcessor ip, int finalWidth, int depth, boolean trace_inside) {
        
    	
    	if (depth < 3) {
            depth = 3;
        }
    	
    	//make sure that the depth is an even number.
    	if ( depth % 2 != 0 )
    	{
    		depth++;
    	}
    	
    	int half_depth = (int) (depth/2);
        PolygonRoi proi = getPolygonRoi(getMask());
        if (proi == null) {
            return null;
        }
        
        Straightener straightener = new Straightener();
        ImagePlus sigImp = new ImagePlus("", ip);
        sigImp.setRoi(proi);
        ImageProcessor sig = straightener.straighten(sigImp, proi, depth);
        FloatPolygon fPoly = proi.getFloatPolygon();
        FloatProcessor xp = new FloatProcessor(fPoly.npoints, depth);
        FloatProcessor yp = new FloatProcessor(fPoly.npoints, depth);
        for (int x = 0; x < fPoly.npoints; x++) {
            for (int y = 0; y < depth; y++) {
                xp.putPixelValue(x, y, fPoly.xpoints[x]);
                yp.putPixelValue(x, y, fPoly.ypoints[x]);
            }
        }
        
        //determine if PolygonRoi points are connected clockwise or counterclockwise
        //algorithm is based on Green's Theorem
        //https://stackoverflow.com/questions/1165647/how-to-determine-if-a-list-of-polygon-points-are-in-clockwise-order
        
        double roi_sum=0;
        for (int x = 0; x < (fPoly.npoints-1); x++) {
        	roi_sum=roi_sum+((fPoly.xpoints[x+1]-fPoly.xpoints[x])*(fPoly.ypoints[x+1]+fPoly.ypoints[x]));
        }
        boolean clockwise;
        if(roi_sum>0)
        {
        	clockwise=true;
        }
        else
        {
        	clockwise=false;
        }
        
        
        
        xp.setInterpolate(true);
        xp.setInterpolationMethod(ImageProcessor.BILINEAR);
        yp.setInterpolate(true);
        yp.setInterpolationMethod(ImageProcessor.BILINEAR);
        sig.setInterpolate(true);
        sig.setInterpolationMethod(ImageProcessor.BILINEAR);
        ImageProcessor sig2 = sig.resize(finalWidth, depth);
        ImageProcessor xp2 = xp.resize(finalWidth, depth);
        ImageProcessor yp2 = yp.resize(finalWidth, depth);
        
        
        
        
        float points1[][] = new float[finalWidth][3];
        //double total_sum1 = 0.0;
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = 0; y < half_depth; y++) {
                sum += sig2.getPixelValue(x, y);
                //total_sum1 += sig2.getPixelValue(x, y);
            }
            points1[x] = new float[]{(float) xp2.getPixelValue(x, 0), (float) yp2.getPixelValue(x, 0), (float) (sum / half_depth)};
        }
        
        float points2[][] = new float[finalWidth][3];
        //double total_sum2 = 0.0;
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = half_depth; y < depth; y++) {
                sum += sig2.getPixelValue(x, y);
                //total_sum2 += sig2.getPixelValue(x, y);
            }
            points2[x] = new float[]{(float) xp2.getPixelValue(x, 0), (float) yp2.getPixelValue(x, 0), (float) (sum / half_depth)};
        }
        
        if(clockwise)
        {
            if(trace_inside)
            {
            	return points1;
            }
            else
            {
            	return points2;
            }
        }
        else
        {
            if(trace_inside)
            {
            	return points2;
            }
            else
            {
            	return points1;
            }
        }
    }

    public void clearBorderPix() {
        borderPix.clear();
    }

    public void loadPixels(LinkedList<short[]> borderPix) {
        this.borderPix = new LinkedList<>(borderPix);
    }

    public ArrayList<float[]> getCentres() {
        if (centres.size() < 1) {
            calcCentroid(getMask());
        }
        return centres;
    }

    public float[] getCentre() {
        ArrayList<float[]> c = getCentres();
        return c.get(c.size() - 1);
    }

    public short[] findSeed(ImageProcessor input) {
//        IJ.saveAs((new ImagePlus("", input)), "PNG", "C:/users/barryd/adapt_debug/input.png");
        int bx = 0, by = 0;
        if (bounds != null) {
            bounds = checkBounds(bounds);
            input.setRoi(bounds);
            bx = bounds.x;
            by = bounds.y;
        }
        ImageProcessor mask = input.crop();
        mask.invert();
        EDM edm = new EDM();
        edm.toEDM(mask);
        int[] max = Utils.findImageMaxima(mask);
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barryd/adapt_debug/edm.png");
        if (!(max[0] < 0.0 || max[1] < 0.0)) {
            return new short[]{(short) (max[0] + bx), (short) (max[1] + by)};
        } else {
            return null;
        }
    }

    public Rectangle checkBounds(Rectangle bounds) {
        if (bounds.x < 0) {
            bounds.x = 0;
        }
        if (bounds.y < 0) {
            bounds.y = 0;
        }
        if (bounds.x + bounds.width > imageWidth) {
            bounds.width = imageWidth - bounds.x;
        }
        if (bounds.y + bounds.height > imageHeight) {
            bounds.height = imageHeight - bounds.y;
        }
        return bounds;
    }

    public boolean morphFilter(int iterations, boolean interpolate, int index, int mode) {
        if (mask == null) {
            mask = getMask();
        }
        for (int i = 0; i < iterations; i++) {
            switch (mode) {
                case ImageProcessor.MIN:
                    mask.dilate();
                    break;
                case ImageProcessor.MAX:
                    mask.erode();
                    break;
            }
        }
        short[][] newBorder = getOrderedBoundary(mask.getWidth(), mask.getHeight(), mask, getCentre());
        if (newBorder == null) {
            return false;
        }
        borderPix = new LinkedList<short[]>();
        for (int j = 0; j < newBorder.length; j++) {
            addBorderPoint(newBorder[j], mask);
        }
        return true;
    }

    public void setFinalMask() {
        mask.setRoi(bounds);
        mask = mask.crop();
    }

    ImageProcessor constructFullSizeMask(int width, int height) {
        ByteProcessor m = new ByteProcessor(width, height);
        m.setColor(Region.MASK_BACKGROUND);
        m.fill();
        m.copyBits(mask, bounds.x, bounds.y, Blitter.AND);
        return m;
    }

    public int getIndex() {
        return index;
    }

    public void addPoint(Pixel p) {
        if (mask == null) {
            drawMask(imageWidth, imageHeight);
        }
        pix.add(p);
        mask.drawPixel(p.getRoundedX(), p.getRoundedY());
        updateBounds(new short[]{(short) p.getX(), (short) p.getY()});
    }

    public boolean addMorphMeasure(double measure) {
        if (morphMeasures == null) {
            morphMeasures = new ArrayList<>();
        }
        return morphMeasures.add(measure);
    }

    public ArrayRealVector getMorphMeasures() {
        Double[] mm = morphMeasures.toArray(new Double[]{});
        ArrayRealVector v = new ArrayRealVector(mm.length);
        for (int i = 0; i < mm.length; i++) {
            v.setEntry(i, mm[i]);
        }
        return v;
    }
}
