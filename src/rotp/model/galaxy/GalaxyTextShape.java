/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.galaxy;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import rotp.model.game.IGameOptions;

// modnar: custom map shape, Text
public class GalaxyTextShape extends GalaxyShape {
    public static final List<String> options1;
    public static final List<String> options2;
    private static final long serialVersionUID = 1L;
    static {
        options1 = new ArrayList<>();
        options1.add("ROTP"); // For the initial setting
        options2 = new ArrayList<>();
        options2.add("SETUP_1_LINE");
        options2.add("SETUP_2_LINE");
        options2.add("SETUP_3_LINE");
    }
	
    private float aspectRatio;
    private float shapeFactor;
    private float adjust_line;
    private double textW;
    private double textH;
    private Shape textShape;
    private Font font;
	
	private Font font() {
    	if (font == null) {
			font = galaxyFont(96);
    	}
    	return font;
    }
    public GalaxyTextShape(IGameOptions options) {
    	super(options);
    }
    @Override protected float   minEmpireFactor() { return 4f; }
    @Override protected boolean allowExtendedPreview()  { return false; }
    @Override public void clean()  { font = null; }
    @Override
    public List<String> options1()  { return options1; }
    @Override
    public List<String> options2()  { return options2; }
    @Override
    public String defaultOption1()  { return options1.get(0); }
    @Override
    public String defaultOption2()  { return options2.get(0); }
    @Override
	public void init(int n) {
        super.init(n);
        
        // int option1 = max(0, options1.indexOf(opts.selectedGalaxyShapeOption1()));
        // int option2 = max(0, options2.indexOf(opts.selectedGalaxyShapeOption2()));
        
		BufferedImage img = new BufferedImage(16, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
//		// Monospaced font used for constant spacing
//		// but maybe other fonts have better kerning for connectivity?
//      Font font1 = new Font(Font.MONOSPACED, Font.PLAIN, 96);
//
//		Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
//		// use TextAttribute.TRACKING to cram letters together for better connectivity
//		attributes.put(TextAttribute.TRACKING, -0.15);
//		Font font2 = font1.deriveFont(attributes);
		
		// modnar: choose text string with option1
		// TODO: work out true multi-line text
		// some text strings will have issues with connectivity regardless of TextAttribute.TRACKING
//        switch(option1) {
//            case 0: {
//                // User-input Homeworld name, user can change colony name afterwards in-game
//                String custStr = text(opts.selectedHomeWorldName());
//                if (custStr.isBlank())
//                	custStr = "!!!Blank!!!";
//                GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), custStr);
//                textShape = v.getOutline();
//                break;
//            }
//            case 1: {
//                // "ROTP"
//                GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), "ROTP");
//                textShape = v.getOutline();
//                break;
//            }
//            case 2: {
//                // "MoO1", using unicode homoglyphs
//                GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), "ℳo○𝟏");
////                GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), "ℳoO1");
//                textShape = v.getOutline();
//                break;
//            }
//        }

//		String galaxyText = opts.selectedGalaxyShapeOption1();
		String galaxyText = finalOption1;
        if (galaxyText.trim().isEmpty())
        	galaxyText = "!!!Blank!!!";
        textShape = font().createGlyphVector(g2.getFontRenderContext(), galaxyText).getOutline();
        
        // modnar: choose number of times to repeat text string with option2
        switch(option2) {
            case 1:
                // repeat twice, 2 lines, with some in-bewteen spacing
                adjust_line = 2.05f;
                break;
            case 2:
                // repeat thrice, 3 lines, with some in-bewteen spacing
                adjust_line = 3.10f;
                break;
            case 0:
            default:
                // repeat once, 1 line
                adjust_line = 1.0f;
                break;
        }
        textW = textShape.getBounds().getWidth();
        textH = textShape.getBounds().getHeight() * adjust_line;
        
		// set galaxy aspect ratio to the textShape aspect ratio
		// this accommodates very long or short text strings
		// for multi-line texts, use adjust_line
        aspectRatio = (float) (textW / textH);
        shapeFactor = sqrt(max(aspectRatio, 1/aspectRatio));
        
        // reset w/h vars since aspect ratio may have changed
        initWidthHeight();
		
		// rescale textShape to fit galaxy map, then move into map center
		AffineTransform scaleText = new AffineTransform();
		AffineTransform moveText = new AffineTransform();
		
		// rescale
		double zoom;
		if (shapeFactor > 1.0) { // Use Zoom X
			zoom = galaxyWidthLY() / textW;
		} else { // Use Zoom Y
			zoom = galaxyHeightLY() / textH;
		}
		// double zoomX = (galaxyWidthLY() - 4*galaxyEdgeBuffer()) / textShape.getBounds().getWidth();
        // zoomY changes with multiple lines
		// double zoomY = (1.0f/adjust_line)*(galaxyHeightLY() - 4*galaxyEdgeBuffer()) / textShape.getBounds().getHeight();
		// double zoom = Math.min(zoomX, zoomY);
		zoom = Math.max(0.1	, zoom); // min result in too munch attempt!
		scaleText.scale(zoom, zoom);
		textShape = scaleText.createTransformedShape(textShape);
		
		// recenter with multiple lines
		double oldX = textShape.getBounds().getX();
		double oldY = textShape.getBounds().getY();
        double moveX = (galaxyWidthLY()-textShape.getBounds().getWidth())/2 - oldX + galaxyEdgeBuffer();
        double moveY = (galaxyHeightLY()-textShape.getBounds().getHeight())/2 - oldY + galaxyEdgeBuffer();
		moveText.translate(moveX, moveY);
		textShape = moveText.createTransformedShape(textShape);
	}
	
    @Override
    public float maxScaleAdj() { return 0.95f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(1.4f*aspectRatio*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() {
        return (int) (Math.sqrt(1.4f*(1/aspectRatio)*opts.numberStarSystems()*adjustedSizeFactor()));
    }
//    @Override
//    public void setRandom(Point.Float pt) {
//        pt.x = randomLocation(fullWidth, galaxyEdgeBuffer());
//        pt.y = randomLocation(fullHeight, galaxyEdgeBuffer());
//    }
    @Override
    public void setSpecific(Point.Float pt) { // modnar: add possibility for specific placement of homeworld/orion locations
        setRandom(pt);
    }
    @Override
    public boolean valid(float x, float y) {
        // modnar: check validity of point with multiple lines
        int option2 = max(0, options2.indexOf(opts.selectedGalaxyShapeOption2()));
        if (option2 == 1) {
            // repeat twice, 2 lines, with some in-between spacing
            return (textShape.contains(x, y+textShape.getBounds().getHeight()*(adjust_line-1)/2)
            		|| textShape.contains(x, y-textShape.getBounds().getHeight()*(adjust_line-1)/2));
        }
        else if (option2 == 2) {
            // repeat thrice, 3 lines, with some in-between spacing
            return (textShape.contains(x, y) 
            		|| textShape.contains(x, y+textShape.getBounds().getHeight()*(adjust_line-1)/2) 
            		|| textShape.contains(x, y-textShape.getBounds().getHeight()*(adjust_line-1)/2));
        }
        // repeat once, 1 line
        return textShape.contains(x, y);
    }
    @Override
    protected float sizeFactor(String size) { return settingsFactor(1.0f); }

//   @Override float randomLocation(float max, float buff) {
//        return buff + (random() * (max-buff-buff));
//    }
//    @Override
//    protected float sizeFactor(String size) {
//        float adj = densitySizeFactor(size);
//		  switch (opts.selectedStarDensityOption()) {
//			  case IGameOptions.STAR_DENSITY_LOWEST:  adj = 1.3f; break;
//			  case IGameOptions.STAR_DENSITY_LOWER:   adj = 1.2f; break;
//			  case IGameOptions.STAR_DENSITY_LOW:     adj = 1.1f; break;
//			  case IGameOptions.STAR_DENSITY_HIGH:    adj = 0.9f; break;
//			  case IGameOptions.STAR_DENSITY_HIGHER:  adj = 0.8f; break;
//			  case IGameOptions.STAR_DENSITY_HIGHEST: adj = 0.7f; break;
//		  }
//        switch (opts.selectedGalaxySize()) {
//        	  case IGameOptions.SIZE_MICRO:     return adj*8; 
//            case IGameOptions.SIZE_TINY:      return adj*10; 
//            case IGameOptions.SIZE_SMALL:     return adj*15; 
//            case IGameOptions.SIZE_SMALL2:    return adj*17;
//            case IGameOptions.SIZE_MEDIUM:    return adj*19; 
//            case IGameOptions.SIZE_MEDIUM2:   return adj*20; 
//            case IGameOptions.SIZE_LARGE:     return adj*21; 
//            case IGameOptions.SIZE_LARGE2:    return adj*22; 
//            case IGameOptions.SIZE_HUGE:      return adj*23; 
//            case IGameOptions.SIZE_HUGE2:     return adj*24; 
//            case IGameOptions.SIZE_MASSIVE:   return adj*25; 
//            case IGameOptions.SIZE_MASSIVE2:  return adj*26; 
//            case IGameOptions.SIZE_MASSIVE3:  return adj*27; 
//            case IGameOptions.SIZE_MASSIVE4:  return adj*28; 
//            case IGameOptions.SIZE_MASSIVE5:  return adj*29; 
//            case IGameOptions.SIZE_INSANE:    return adj*32; 
//            case IGameOptions.SIZE_LUDICROUS: return adj*36; 
//            default:             return adj*19; 
//        }
//    }
}
