# Adapt edge analysis modification - inside cell borders
Adjusted ADAPT plugin code by Leif Dehmelt

This repository contains code to modify the ADAPT plugin originally publshed by Barry et al:
http://dx.doi.org/10.1083/jcb.201501081

The original code is available at:
https://github.com/djpbarry/Adapt
and
https://github.com/djpbarry/IAClassLibrary

After implementing this modification, the analysis area is changed to only include data inside the cell borders.

Changes in iaclasslibrary-1.0.6:

An additional implementation of the buildMapCol function was added to the class 

IAClassLibrary-master\src\main\java\net\calm\iaclasslibrary\IAClasses\Region.java:

public float[][] buildMapCol(ImageProcessor ip, int finalWidth, int depth, boolean trace_inside)


This implementation is a variant of the existing buildMapCol function of the iaclasslibrary:

public float[][] buildMapCol(ImageProcessor ip, int finalWidth, int depth)

The new implementation allows the generation of a region that is only inside the cell border. For this, the original region is divided into two halves, which correspond to the region inside and outside the border. To determine which half is inside, the clockwise or anti-clockwise arrangement of the points for the region is determined by an algorithm that is based on Green's Theorem.

The full code of the modified function starting in line 606 of Region.java is as follows:


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

Changes in adapt-3.0.1:

In the class 

Adapt-master\src\main\java\net\calm\adapt\Adapt\Analyze_Movie.java

the line 1560 was commented out to display only the region inside the cell border:

original:

enlargedMask.dilate();

edit:

//enlargedMask.dilate();

In the class 

Adapt-master\src\main\java\net\calm\adapt\Output\RunnableOutputGenerator.java

the line 494 was was changed to point to the new buildMapCol implementation that enables the restriction of analysis to the central cell area:

original:
smPoints = current.buildMapCol(sigStack.getProcessor(i + 1), height,
                		(int) Math.round(uv.getCortexDepth() / uv.getSpatialRes()));

edit:
smPoints = current.buildMapCol(sigStack.getProcessor(i + 1), height,
                		(int) Math.round(uv.getCortexDepth() / uv.getSpatialRes()),true);

