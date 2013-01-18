package openpool;
import monclubelec.javacvPro.*;
import SimpleOpenNI.*;
import java.awt.*;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;

class BackGroundDiff
{
  // OpenCV
  SimpleOpenNI kinect1;
  SimpleOpenNI kinect2;

  OpenCV opencv;
  Blob[] blobsArray = null;
  ArrayList<Point> bgPoints;
  float threshold = 0.1f;
  PImage depthImage;
  int[] depthMap;
  int depth_width;
  int depth_height;

  int   pos[][] = {
    {
      50, 50
    }
    , {
      640+50, 240+50
    }
  };

  BackGroundDiff(SimpleOpenNI kinect1, SimpleOpenNI kinect2)
  {
    this.kinect1 = kinect1;
    this.kinect2 = kinect2;

    bgPoints = new ArrayList<Point>();

    //kinect.enableRGB(); // カラー画像有効化
    kinect1.update();
    kinect2.update();

    depth_width  = kinect1.depthWidth() + kinect2.depthWidth();
    depth_height = kinect1.depthHeight();

    depthImage = combineDepthImage(kinect1.depthImage(), kinect2.depthImage(), 0, 0, 0, 0);
    depthMap   = combineDepthMap( kinect1.depthMap(), kinect2.depthMap(), kinect1.depthImage(), kinect2.depthImage());

    opencv = new OpenCV();
    opencv.allocate(depth_width, depth_height);
    rememberBackground();

    update();
  }

  void update()  
  {    
    kinect1.update();
    kinect2.update();

    depthImage = retrieveDepthImage();

    // Calculate the diff image
    opencv.copy(depthImage);

    opencv.absDiff(); // result stored in the secondary memory.
    opencv.restore2(); // restore the secondary memory data to the main buffer
    opencv.blur(3);
    opencv.threshold(threshold, "BINARY");
    depthImage = opencv.getBuffer();
    depthImage = DilateWhite(depthImage, 5);

    // Detect blobs
    opencv.copy(depthImage);
    blobsArray = opencv.blobs(200, 800, 15, false, 100);
  }

  void rememberBackground()
  {
    System.out.println("remember background!!!");
    opencv.copy(retrieveDepthImage());
    opencv.remember(); // Store in the first buffer.
  }

  PImage retrieveDepthImage()
  {
    PImage depthImage = combineDepthImage(kinect1.depthImage(), kinect2.depthImage(), 0, 0, 0, 0);
    int[] depthMap = combineDepthMap(kinect1.depthMap(), kinect2.depthMap(), kinect1.depthImage(), kinect2.depthImage());

    // Assume depth errors are caused by the black ball
    int white = 0xffffff;
    for (int x = 0; x < depth_width; x ++)
    {
      for (int y = 0; y < depth_height; y ++)
      {
        if (depthMap[x + y * depth_width] <= 0)
        {
          depthImage.set(x, y, white);
        }
      }
    }
    return depthImage;
  }

  PImage DilateWhite(OpenPool op, PImage in, int times)
  {
    int BLACK = 0;
    int WHITE = 0xffffff;
    PImage out;
    out = in.get();

    for (int t=0; t<times;t++)
    {
      //
      for (int i=0;i<in.width;i++)
      {
        out.set(i, 0, BLACK);
        out.set(i, in.height, BLACK);
      }
      for (int j=0;j<in.height;j++)
      {
        out.set(0, j, BLACK);
        out.set(in.width, j, BLACK);
      }

      for (int i = 1 ; i < in.width-1 ; i++)
      {
        for (int j = 1 ; j < in.height-1 ; j++ )
        {
          if (
          in.get(i-1, j-1) == WHITE &&
            in.get(i, j-1) == WHITE &&
            in.get(i+1, j-1) == WHITE &&
            in.get(i-1, j) == WHITE &&
            in.get(i+1, j) == WHITE &&
            in.get(i-1, j+1) == WHITE &&
            in.get(i, j+1) == WHITE &&
            in.get(i+1, j+1) == WHITE 
            )
          {
            out.set(i, j, WHITE);
          }
          else
          { 
            out.set(i, j, BLACK);
          }
        }
      }
    }
    return out;
  }

  void draw(OpenPool op)
  {
    bgPoints.clear();    


    for (Blob blob:blobsArray)
    {
      Point pt = new Point();
      pt.x = (blob.centroid.x* (pos[1][0]-pos[0][0])) / depthImage.width + pos[0][0];
      pt.y = (blob.centroid.y* (pos[1][1]-pos[0][1])) / depthImage.height + pos[0][1]; 
      bgPoints.add(pt);

      if (OpenPool.DEBUG)
      {
        op.pa.line(pt.x-50, pt.y, pt.x+50, pt.y);
        op.pa.line(pt.x, pt.y-50, pt.x, pt.y+50);
        text( str(blob.area), pt.x, pt.y-30);
      }
    }

    if (OpenPool.DEBUG)
    {
      op.pa.stroke(255,255,  0);
      op.pa.fill(255,255, 0);
      //draw image boundingbox
      op.pa.line(pos[0][0],pos[0][1],pos[1][0],pos[0][1]);
      op.pa.line(pos[1][0],pos[0][1],pos[1][0],pos[1][1]);
      op.pa.line(pos[1][0],pos[1][1],pos[0][0],pos[1][1]);
      op.pa.line(pos[0][0],pos[1][1],pos[0][0],pos[0][1]);
      
      //draw an arrow

      op.pa.line(pos[0][0], pos[0][1], pos[0][0]+8, pos[0][1]+4);
      op.pa.line(pos[0][0], pos[0][1], pos[0][0]+4, pos[0][1]+8);
      op.pa.line(pos[0][0], pos[0][1], pos[0][0]+10, pos[0][1]+10);

      //draw xy
      op.pa.text("X:", pos[0][0]+20, pos[0][1]+20);
      op.pa.text(pos[0][0], pos[0][0]+30, pos[0][1]+20);
      op.pa.text("Y:", pos[0][0]+20, pos[0][1]+30);
      op.pa.text(pos[0][1], pos[0][0]+30, pos[0][1]+30);

      //draw an arrow
      op.pa.line(pos[1][0], pos[1][1], pos[1][0]-8, pos[1][1]-4);
      op.pa.line(pos[1][0], pos[1][1], pos[1][0]-4, pos[1][1]-8);
      op.pa.line(pos[1][0], pos[1][1], pos[1][0]-10, pos[1][1]-10);

      //draw xy
      op.pa.text("X:", pos[1][0]-50, pos[1][1]-10);
      op.pa.text(pos[1][0], pos[1][0]-40, pos[1][1]-10);
      op.pa.text("Y:", pos[1][0]-50, pos[1][1]-20);
      op.pa.text(pos[1][1], pos[1][0]-40, pos[1][1]-20);

      op.pa.noFill();
      op.pa.stroke(255, 255, 255);

      //draw depthimage
      op.pa.image(depthImage, pos[0][0], pos[0][1], pos[1][0]-pos[0][0], pos[1][1]-pos[0][1]);
    }
  }

  PImage combineDepthImage(PImage img1, PImage img2, int img1x, int img1y, int img2x, int img2y)
  {
    PImage retImage;
    retImage = createImage(img1.width+img2.width, img1.height, PImage.ARGB);
    retImage.loadPixels();

    //put img1 into retImage;
    if (img1y >= 0)
    {
      for (int i=0; i<(img1.height-img1y); i++)
      {
        if (img1x >= 0)
        {
          for (int j=0; j<(img1.width-img1x);j++)
          {
            retImage.pixels[(retImage.width*(i+img1y))+(j+img1x)] = img1.pixels[img1.width*i+j];
          }
        }
        else
        {
          for (int j= -img1x; j<(img1.width);j++)
          {
            retImage.pixels[(retImage.width*(i+img1y))+(j+img1x)] = img1.pixels[img1.width*i+j];
          }
        }
      }
    }

    else
    {
      for (int i= -img1y; i<img1.height; i++)
      {
        if (img1x >= 0)
        {
          for (int j=0; j<(img1.width-img1x);j++)
          {
            retImage.pixels[(retImage.width*(i+img1y))+(j+img1x)] = img1.pixels[img1.width*i+j];
          }
        }
        else
        {
          for (int j= -img1x; j<(img1.width);j++)
          {
            retImage.pixels[(retImage.width*(i+img1y))+(j+img1x)] = img1.pixels[img1.width*i+j];
          }
        }
      }
    }

    //put img2 into retImage
    if (img2y >= 0)
    {
      for (int i=0; i<(img2.height-img2y); i++)
      {
        if (img2x >= 0)
        {
          for (int j=0; j<(img2.width-img2x);j++)
          {
            retImage.pixels[(retImage.width*(i+img2y))+(j+img2x)+img1.width] = img2.pixels[img2.width*i+j];
          }
        }
        else
        {
          for (int j= -img2x; j<(img2.width);j++)
          {
            retImage.pixels[(retImage.width*(i+img2y))+(j+img2x)+img1.width] = img2.pixels[img2.width*i+j];
          }
        }
      }
    }

    else
    {
      for (int i= -img2y; i<img2.height; i++)
      {
        if (img2x >= 0)
        {
          for (int j=0; j<(img2.width-img2x);j++)
          {
            retImage.pixels[(retImage.width*(i+img2y))+(j+img2x)+img1.width] = img2.pixels[img2.width*i+j];
          }
        }
        else
        {
          for (int j= -img2x; j<(img2.width);j++)
          {
            retImage.pixels[(retImage.width*(i+img2y))+(j+img2x)+img1.width] = img2.pixels[img2.width*i+j];
          }
        }
      }
    }
    return retImage;
  };

  int[] combineDepthMap(int[] map1, int[] map2, PImage img1, PImage img2)
  {
    PImage retImage;
    int[] ret = new int[(img1.width+img2.width)*img1.height];



    for (int i=0; i<img1.height; i++)
    {
      for (int j=0; j<img1.width;j++)
      {
        ret[((img1.width+img2.width)*i)+j] = map1[img1.width*i+j];
      }
      for (int j=0; j<img2.width;j++)
      {
        ret[((img1.width+img2.width)*i)+img1.width+j] = map2[img2.width*i+j];
      }
    }
    return ret;
  };
}

