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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import rotp.model.Sprite;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.tech.TechArmor;
import rotp.model.tech.TechBattleSuit;
import rotp.model.tech.TechHandWeapon;
import rotp.model.tech.TechPersonalShield;
import rotp.model.tech.TechTree;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.notifications.TransportsCapturedAlert;
import rotp.ui.notifications.TransportsPerishedAlert;
import rotp.ui.sprites.FlightPathSprite;
import rotp.util.Base;

public class Transport extends FleetBase implements Base, Ship, Sprite, Serializable {
    private static final long serialVersionUID = 1L;
    private Empire empire;
    private final StarSystem from;
    private StarSystem dest;
    private Empire targetEmp;
    private int size;
    private int originalSize;
    private float hitPoints;
    private float speed;
    private float combatSpeed;
    private float combatTransportPct;
    private float combatAdj = 0;
    private float launchTime = NOT_LAUNCHED;
    private float travelSpeed = 0;

    private String troopArmorId;
    private String troopBattleSuitId;
    private String troopWeaponId;
    private String troopShieldId;
    private transient Rectangle selectBox;
    private transient boolean hovering;
    private transient FlightPathSprite pathSprite;
    private transient StarSystem hoveringDest;


    public Empire empire()                   { return empire; }
    private TechArmor troopArmor()           { return (TechArmor) tech(troopArmorId); }
    private TechBattleSuit troopBattleSuit() { return (TechBattleSuit) tech(troopBattleSuitId); }
    private TechHandWeapon troopWeapon()     { return (TechHandWeapon) tech(troopWeaponId); }
    private TechPersonalShield troopShield() { return (TechPersonalShield) tech(troopShieldId); }
    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public int displayPriority()                { return 8; }
    @Override
    public boolean hasDisplayPanel()            { return true; }
    @Override
    public boolean persistOnClick()             { return empire() == player(); }    
    public boolean surrenderOnArrival()         { return size < 0; }
    
    public void orderToSurrenderOnArrival()    {  size = -1; }
    public void toggleSurrenderOnArrival() {
        if (surrenderOnArrival())
            size = originalSize;
        else 
            orderToSurrenderOnArrival();
    }

    public Rectangle selectBox() {
        if (selectBox == null)
            selectBox = new Rectangle();
        return selectBox;
    }
    @Override
    public FlightPathSprite pathSprite() {
        if (pathSprite == null)
            pathSprite = new FlightPathSprite(this, destination());
        return pathSprite;
    }

    public Transport(StarSystem sys) {
        from = sys;
        empire = sys.empire();
    }
    @Override
    public String toString()          { return concat("Transport: ", Integer.toHexString(hashCode())); }
    public Colony home()              { return from.colony(); }
    public StarSystem destination()   { return dest; }
    public void setDest(StarSystem d) { dest = d; }
    public StarSystem from()          { return from; }
    @Override
    public float fromX()              { return from.x(); }
    @Override
    public float fromY()              { return from.y(); }
    public int size()                 { return size; }
    public void size(int s)           { size = s; }
    public int launchSize()           { return originalSize; }
    public void launchSize(int s  )   { originalSize = s; }
    public float hitPoints()          { return hitPoints; }
    public float combatTransportPct() { return combatTransportPct; }
    public float combatAdj()          { return combatAdj; }
    @Override
    public float launchTime()         { return launchTime; }
    public Empire targetCiv()         { return targetEmp; }
    public float travelSpeed()        { return travelSpeed; }
    public void travelSpeed(float d)  { travelSpeed = d; }


    public void reset(Empire emp) {
        size = 0;
        dest = null;
        empire = emp;
    }
    @Override
    public int destSysId()              { return dest == null ? StarSystem.NULL_ID : dest.id; }
    @Override
    public int empId()                  { return empire.id; }
    public void arrive()                { dest.acceptTransport(this); }
    @Override
    public boolean visibleTo(int empId) { return true; }
    @Override
    public boolean isTransport()       { return true; }

    // MappedObject overrides
    @Override
    public float x() { return inTransit() ? transitX() : from.x();  }
    @Override
    public float y() { return inTransit() ? transitY() : from.y();  }
    public boolean launched()       { return launchTime > NOT_LAUNCHED; }
    @Override
    public boolean deployed()       { return launched(); }
    @Override
    public boolean inTransit()     { return dest != null; }
    public boolean isActive()      { return (size > 0) && (dest != null) && (dest != home().starSystem()); }
    @Override
    public boolean isPotentiallyArmed(Empire e) {
        return empire != e;
    }

    public void launch()    {
        if (launched())
            return;

        Galaxy gal = galaxy();
        // if being sent back to home system, then abort the launch
        if (dest == from) {
            size = 0;
            gal.removeTransport(this);
            return;
        }

        // one last sanity check on size
        size = min(size, (int) Math.ceil(from.colony().population()));
        originalSize = size;
        launchTime = gal.currentTime();
        targetEmp = dest.empire();
        speed = empire.tech().transportTravelSpeed();
        combatSpeed = empire.tech().transportCombatSpeed();
        setArrivalTime();

        gal.addTransport(this);
        TechTree tech = empire.tech();

        hitPoints = tech.topArmorTech().transportHP;
        combatAdj = tech.troopCombatAdj(false);
        combatTransportPct = empire.combatTransportPct();

        troopArmorId = tech.topArmorTech().id;
        troopBattleSuitId = tech.topBattleSuitTech().id;
        troopWeaponId = tech.topHandWeaponTech().id;
        troopShieldId = tech.topPersonalShieldTech().id;
    }
    public void setDefaultTravelSpeed() {
        travelSpeed = from.canStargateTravelTo(dest) ? distanceTo(dest) : empire.transportTravelSpeed(from, dest);
    }
    @Override
    public boolean canSendTo(int sysId) {
        return empire.sv.withinRange(sysId, range());
    }
    @Override
    public boolean validDestination(int sysId) {
        return empire.sv.isScouted(sysId) && empire.sv.isColonized(sysId) && empire.canColonize(sysId) && canSendTo(sysId);
    }
    public int gauntletRounds() {
        switch((int)combatSpeed) {
            case 0: case 1: case 2: case 3: case 4:
                return 4;
            case 5: case 6:
                return 3;
            case 7: case 8:
                return 2;
            case 9: default:
                return 1;
        }
    }

    public float range()                        { return empire.tech().shipRange(); }
    public float travelTime(StarSystem dest)    { 
        float normalTime;
        if (speed == 0)
            normalTime = travelTime(this, dest, empire().tech().transportTravelSpeed());
        else
            normalTime = travelTime(this, dest, speed); 
        
        if ((from.empire() == dest.empire()) && (from.empire() == empire)
        && from.colony().shipyard().hasStargate() && dest.colony().shipyard().hasStargate())
            return min(1, normalTime);
        else
            return normalTime;
    }
    public float travelTime(IMappedObject fr, StarSystem to) {
        //float speed = empire.transportSpeed(fr, to);
        float normalTime = travelTime(fr ,to, travelSpeed);

        if (fr instanceof StarSystem) {
            StarSystem fromSystem = (StarSystem) fr;
            if ((fromSystem.empire() == to.empire()) && (fromSystem.empire() == empire)
            && fromSystem.colony().shipyard().hasStargate() && to.colony().shipyard().hasStargate())
                return min(1, normalTime);
        }
        return normalTime;
    }
    public float calculateArrivalTime() {
        // direct time is if we go straight there at empire's tech transport speed
        float directTime = travelTime(dest);
        // set time is if we have travelSpeed alrady set, by synching transports
        float setTime = travelSpeed > 0 ? distanceTo(dest)/travelSpeed : directTime;
        // take the worst time
        return galaxy().currentTime() + max(setTime, directTime);
    }
    public boolean  changeDestination(StarSystem to) {
        if (inTransit()
        && validDestination(id(to))) {
            dest = to;
            targetEmp = to.empire();
            setArrivalTime();
            return true;
        }
        return false;
    }
    public void joinWith(Transport tr) {
        size += tr.size;
        originalSize += tr.originalSize;
        hitPoints = Math.max(hitPoints, tr.hitPoints);
        speed = max(speed, tr.speed);
        combatSpeed = max(combatSpeed, tr.combatSpeed);
        combatAdj = Math.max(combatAdj, tr.combatAdj);
        combatTransportPct = Math.max(combatTransportPct, tr.combatTransportPct);

        // use best technologies of the transports
        if (tr.troopArmor().level > troopArmor().level)
            troopArmorId = tr.troopArmorId;

        if (tr.troopBattleSuit().level > troopBattleSuit().level)
            troopBattleSuitId = tr.troopBattleSuitId;

        if (troopWeaponId == null)
            troopWeaponId = tr.troopWeaponId;
        else if ((tr.troopWeaponId != null) && (tr.troopWeapon().level > troopWeapon().level))
            troopWeaponId = tr.troopWeaponId;

        if (troopShieldId == null)
            troopShieldId = tr.troopShieldId;
        else if ((tr.troopShieldId != null) && (tr.troopShield().level > troopShield().level))
            troopShieldId = tr.troopShieldId;
    }
    public void land() {
        //there's a chance that our empire was destroyed between the arrival of our transport and it's landing
        if(empire().extinct())
            size = 0;
        else if (!dest.isColonized()) {
            if (dest.abandoned() && !dest.unnamed() && empire.canColonize(dest))
                empire().takeAbandonedSystem(dest, this);
            else {
                log(concat(str(size), " ", empire.name(), " transports perished at ", dest.name()));
                if (empire.isPlayerControlled())
                    TransportsPerishedAlert.create(targetEmp, dest);
                size = 0;
            }
        }
        else if (dest.empire() != empire) {
            if (surrenderOnArrival()) {
                log(concat(str(size), " ", empire.name(), " transports surrendered at ", dest.name()));
                if (empire.isPlayerControlled() || dest.empire().isPlayerControlled())
                    TransportsCapturedAlert.create(empire, dest.empire(), dest, originalSize);
                size = 0;
            }
            else
                dest.colony().resistTransport(this);
        }
        else if (dest.colony().inRebellion())
            dest.colony().resistTransportWithRebels(this);
        else
            dest.colony().acceptTransport(this);

        //Transport is gone. If selected on the map, de-select and replace with target system
        session().replaceVarValue(this, dest);
    }
    public String armorDesc()        { return troopArmor().shortName(); }
    public String battleSuitDesc()   { return troopBattleSuit().name(); }
    public String weaponDesc()       { return troopWeaponId == null ? ""  : troopWeapon().name(); }
    public String shieldDesc()       { return troopShield().name(); }

    @Override
    public IMappedObject source() { return this; }
    @Override
    public int centerMapX(GalaxyMapPanel map)   { return launched() ? mapX(map)+BasePanel.s10 : mapX(map); }
    @Override
    public int centerMapY(GalaxyMapPanel map)   { return launched() ? mapY(map)+BasePanel.s5 : mapY(map); }
    @Override
    public int mapY(GalaxyMapPanel map)         { return launched() ? map.mapY(source().y())+BasePanel.s10 : map.mapY(source().y()); }
    @Override
    public int maxMapScale()                    { return GalaxyMapPanel.MAX_FLEET_TRANSPORT_SCALE;  }
    @Override
    public boolean decideWhetherDisplayed(GalaxyMapPanel map) {
        if (!map.parent().isClicked(this)) {
            if ((empire == targetEmp) && !map.showFriendlyTransports())
                return false;
            if ((empire != targetEmp) && !map.showArmedShips())
                return false;
        }
        if (map.scaleX() > maxMapScale())
            return false;
        return true;
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (!displayed())
            return;
        int x = mapX(map);
        int y = mapY(map);

        BufferedImage img = empire.transportImage();
        int w = img.getWidth();
        int h = img.getHeight();

        int sW = w;
        int sH = h;


        if ((destination() == null)
        || (destination().x() > x()))
            g2.drawImage(img, x, y, x+sW, y+sH, 0, 0, w, h, map);
        else
            g2.drawImage(img, x+sW, y, x, y+sH, 0, 0, w, h, map);

        int s5 = BasePanel.s5;
        int s10 = BasePanel.s10;
        //int s20 = BasePanel.s20;
        int cnr = BasePanel.s10;

        selectBox().setBounds(x-s5,y-s5,sW+s10,sH+s10);

        if (map.parent().isClicked(this))
            drawSelection(g2, map, this, x-s5, y-s5, sW+s10, sH+s10, cnr);
        else if (map.parent().isHovering(this))
            drawHovering(g2, map, this, x-s5, y-s5, sW+s10, sH+s10, cnr);

        if (hoveringDest != null) {
            FlightPathSprite path = pathSpriteTo(starSystem());
            if (path != null)
                path.draw(map, g2);
        }

        if (map.showAllFlightPaths() || map.parent().isClicked(this))
            if (player().knowETA(this))
                pathSprite().draw(map, g2);
            else if (options().selectedTrackUFOsAcrossTurns()) {
                StarSystem suspectedDestination = player().suspectedDestinationsOfVisibleShips().get(this);
                if (suspectedDestination != null)
                    pathSpriteTo(suspectedDestination).draw(map, g2);
            }
    }

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        return map.showFriendlyTransports() && selectBox().contains(mapX, mapY);
    }

    private void drawSelection(Graphics2D g, GalaxyMapPanel map, Transport fl, int x, int y, int w, int h, int cnr) {
        if (fl.empire() == null)
            return;

        g.setColor(fl.empire().selectionColor());
        g.fillRoundRect(x, y, w, h, cnr, cnr);

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.setColor(fl.empire().color());
        g.drawRoundRect(x,y,w,h, cnr, cnr);
        g.setStroke(prev);
    }
    private void drawHovering(Graphics2D g, GalaxyMapPanel map, Transport fl, int x, int y, int w, int h, int cnr) {
        if (fl.empire() == null)
            return;

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.setColor(fl.empire().color());
        g.drawRoundRect(x,y,w,h, cnr, cnr);
        g.setStroke(prev);
    }
    private FlightPathSprite pathSpriteTo(StarSystem sys) {
        return new FlightPathSprite(this, sys);
    }
}
