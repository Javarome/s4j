/*
* Stellarium for Java
* Copyright (c) 2005-2006 Jerome Beau
*
* Java adaptation of <a href="http://www.stellarium.org">Stellarium</a>
* The C++ file was:
* This file Copyright (C) 2005 Robert Spearman
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.stellarium.command;

import org.stellarium.*;
import org.stellarium.astro.JulianDay;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.ui.render.Image;
import org.stellarium.ui.render.ImageMgr;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.stellarium.StelUtility.isEmpty;
import static org.stellarium.StelUtility.stringToJDay;

/**
 * @author <a href="mailto:javarome@javarome.net">J&eacuter&ocirc;me Beau</a>, Fred Simon
 * @version 0.8.2
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_command_interface.h?view=log&pathrev=stellarium-0-8-2">stel_command_interface.h</>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_command_interface.cpp?view=log&pathrev=stellarium-0-8-2">stel_command_interface.cpp</>
 */
public class StelCommandInterface extends CommandInterface {

    public StelCommandInterface(StelApp someApp) {
        stapp = someApp;
    }

    public void executeCommand(String commandline) throws StellariumException {
        long delay = 0;
        executeCommand(commandline, delay, true);// Assumed to be trusted!
        // delay is ignored, as not needed by the ui callers
    }

    /**
     * for easy calling of simple commands with a double as last argument value
     */
    public void executeCommand(String command, String arg) throws StellariumException {
        long delay = 0;
        String commandline = command + arg;
        executeCommand(commandline, delay, true);// Assumed to be trusted!
        // delay is ignored, as not needed by the ui callers
    }

    /**
     * for easy calling of simple commands with a boolean as last argument value
     */
    public void executeCommand(String command, boolean arg) throws StellariumException {
        long delay = 0;
        String commandline = command + (arg ? "1" : "0");
        executeCommand(commandline, delay, true);// Assumed to be trusted!
        // delay is ignored, as not needed by the ui callers
    }

    /**
     * Called by script executors
     * <p/>
     * - TODO details TBD when needed
     *
     * @param commandline
     * @param wait
     * @param trusted     Some key settings can't be modified by scripts unless they are "trusted"
     * @return The new wait time
     * @throws StellariumException If the command was not understood
     */
    long executeCommand(String commandline, long wait, boolean trusted) throws StellariumException {
        Map<String, String> args = new LinkedHashMap<String, String>();
        boolean status;// true if command was understood
        boolean recordable = true;// true if command should be recorded (if recording)
        wait = 0;// default, no wait between commands

        String command = parseCommand(commandline, args);
        status = !isEmpty(command);

        // stellarium specific logic to run each command
        if ("flag".equals(command)) {
            commandline = setFlagCommand(commandline, trusted, args, command);
        } else if ("wait".equals(command) && !args.get("duration").equals("")) {
            wait = setWaitCommand(wait, args);
        } else if ("set".equals(command)) {
            setSetCommand(commandline, trusted, args, command);
        } else if ("select".equals(command)) {
            setSelectCommand(args);
        } else if ("deselect".equals(command)) {
            getCore().unSelect();
        } else if ("look".equals(command)) {
            setLookCommand(args);
        } else if (command.equals("zoom")) {
            setZoomCommand(commandline, args, command);
        } else if ("timerate".equals(command)) {// NOTE: accuracy issue related to frame rate
            commandline = setTimerateCommand(commandline, args, command);
        } else if ("date".equals(command)) {
            setDateCommand(commandline, args, command);
        } else if ("moveto".equals(command)) {
            setMoveToCommand(commandline, args, command);
        } else if ("image".equals(command)) {
            setImageCommand(commandline, args, status);
        } else if (command.equals("audio")) {
            recordable = setAudioCommand(commandline, args, recordable, command);
        } else if ("script".equals(command)) {
            recordable = setScriptCommand(commandline, args, recordable, command);
        } else if ("clear".equals(command)) {
            setClearCommand(args);
        } else if ("landscape".equals(command) && "load".equals(args.get("action"))) {
            setLandscapeCommand(args);
        } else if ("meteors".equals(command)) {
            setMeteorsCommand(commandline, args, command);
        } else if ("configuration".equals(command)) {
            return setConfigurationCommand(commandline);
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized or malformed command: " + command);
        }

        if (recordable) {
            stapp.getScripts().recordCommand(commandline);
        }

        return wait;
    }

    private long setConfigurationCommand(String commandline) {
        throw new StellariumCommandException(commandline, "\"configuration\" command no longer supported.\n");

        // Fabien : this should be useless. If you need to reset an initial state after running a script, just run a reinit script.

        // 	  if(args["action"]=="load" && trusted) {
        // 		  // eventually load/reload are not both needed, but for now this is called at startup, reload later
        // 		  // stapp->loadConfigFrom(stapp->getConfigFile());
        // 		  recordable = 0;  // don't record as scripts can not run this
        //
        // 	  } else if(args["action"]=="reload") {
        //
        // 		  // on reload, be sure to reconfigure as necessary since StelCore::init isn't called
        //
        // 		  stapp->loadConfigFrom(stapp->getConfigFile());
        //
        // 		  if(stcore->asterisms) {
        // 			  stcore->setConstellationArtIntensity(stcore->getConstellationArtIntensity());
        // 			  stcore->setConstellationArtFadeDuration(stcore->getConstellationArtFadeDuration());
        // 		  }
        // 		  if (!stcore->getFlagAtmosphere() && stcore->tone_converter)
        // 			  stcore->tone_converter->set_world_adaptation_luminance(3.75f);
        // 		  //if (stcore->getFlagAtmosphere()) stcore->atmosphere->set_fade_duration(stcore->AtmosphereFadeDuration);
        // 		  stcore->observatory->load(stapp->getConfigFile(), "init_location");
        // 		  stcore->setLandscape(stcore->observatory->get_landscape_name());
        //
        // 		  if (stapp->StartupTimeMode=="preset" || stapp->StartupTimeMode=="Preset")
        // 			  {
        // 				  stcore->navigation->set_JDay(stapp->PresetSkyTime -
        // 											 stcore->observatory->get_GMT_shift(stapp->PresetSkyTime) * JD_HOUR);
        // 			  }
        // 		  else
        // 			  {
        // 				  stcore->navigation->set_JDay(get_julian_from_sys());
        // 			  }
        // 		  if(stcore->getFlagPlanetsTrails() && stcore->ssystem) stcore->startPlanetsTrails(true);
        // 		  else stcore->startPlanetsTrails(false);
        //
        // 		  string temp = stcore->skyCulture;  // fool caching in below method
        // 		  stcore->skyCulture = "";
        // 		  stcore->setSkyCulture(temp);
        //
        // 		  system( ( stcore->getDataDir() + "script_loadConfig " ).c_str() );
        //
        // 	  } else status = 0;
    }

    private void setMeteorsCommand(String commandline, Map<String, String> args, String command) {
        if (!isEmpty(args.get("zhr"))) {
            getCore().setMeteorsRate(strToInt(args.get("zhr")));
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
    }

    private void setLandscapeCommand(Map<String, String> args) {
        // textures are relative to script
        args.put("path", stapp.getScripts().getScriptPath());
        getCore().loadLandscape(args);
    }

    private void setClearCommand(Map<String, String> args) {
        // TODO move to stelcore

        // set sky to known, standard states (used by scripts for simplicity)
        executeCommand("set home_planet Earth");

        if ("natural".equals(args.get("state"))) {
            executeCommand("flag atmosphere on");
            executeCommand("flag landscape on");
        } else {
            executeCommand("flag atmosphere off");
            executeCommand("flag landscape off");
        }

        // turn off all labels
        executeCommand("flag azimuthal_grid off");
        executeCommand("flag meridian_line off");
        executeCommand("flag cardinal_points off");
        executeCommand("flag constellation_art off");
        executeCommand("flag constellation_drawing off");
        executeCommand("flag constellation_names off");
        executeCommand("flag constellation_boundaries off");
        executeCommand("flag ecliptic_line off");
        executeCommand("flag equatorial_grid off");
        executeCommand("flag equator_line off");
        executeCommand("flag fog off");
        executeCommand("flag nebula_names off");
        executeCommand("flag object_trails off");
        executeCommand("flag planet_names off");
        executeCommand("flag planet_orbits off");
        executeCommand("flag show_tui_datetime off");
        executeCommand("flag star_names off");
        executeCommand("flag show_tui_short_obj_info off");

        // make sure planets, stars, etc. are turned on!
        // milkyway is left to user, for those without 3d cards
        executeCommand("flag stars on");
        executeCommand("flag planets on");
        executeCommand("flag nebulae on");

        // also deselect everything, set to default fov and real time rate
        executeCommand("deselect");
        executeCommand("timerate rate 1");
        executeCommand("zoom auto initial");
    }

    private boolean setScriptCommand(String commandline, Map<String, String> args, boolean recordable, String command) {
        ImageMgr scriptImages = getCore().getImageMgr();

        if ("end".equals(args.get("action"))) {
            // stop script, audio, and unload any loaded images
            if (audio != null) {
                audio.close();
                audio = null;
            }
            stapp.getScripts().cancelScript();
            scriptImages.dropAllImages();
        } else if (args.get("action").equals("play") && !isEmpty(args.get("filename"))) {
            if (stapp.getScripts().isPlaying()) {

                String scriptPath = stapp.getScripts().getScriptPath();

                // stop script, audio, and unload any loaded images
                if (audio != null) {
                    audio.close();
                    audio = null;
                }
                stapp.getScripts().cancelScript();
                scriptImages.dropAllImages();

                // keep same script path
                stapp.getScripts().playScript(scriptPath + args.get("filename"), scriptPath);
            } else {
                stapp.getScripts().playScript(args.get("path") + args.get("filename"), args.get("path"));
            }
        } else if (args.get("action").equals("record")) {// TEMP
            //    if(args["action"]=="record" && args["filename"]!="") {
            stapp.getScripts().recordScript(args.get("filename"));
            recordable = false;// don't record this command!
        } else if (args.get("action").equals("cancelrecord")) {
            stapp.getScripts().cancelRecordScript();
            recordable = false;// don't record this command!
        } else if (args.get("action").equals("pause") && !stapp.getScripts().isPaused()) {
            // n.b. action=pause TOGGLES pause
            if (audio != null) audio.pause();
            stapp.getScripts().pauseScript();
        } else if (args.get("action").equals("pause") || args.get("action").equals("resume")) {
            stapp.getScripts().resumeScript();
            if (audio != null) audio.sync();
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
        return recordable;
    }

    private boolean setAudioCommand(String commandline, Map<String, String> args, boolean recordable, String command) {
        /* TODO: Fred find a way to test that
    #ifndef HAVE_SDL_MIXER_H
            debug_message = _("This executable was compiled without audio support.");
            status = 0;
    #else
    */
        if ("sync".equals(args.get("action"))) {
            if (audio != null) audio.sync();
        } else if ("pause".equals(args.get("action"))) {
            if (audio != null) audio.pause();

        } else if ("play".equals(args.get("action")) && !isEmpty(args.get("filename"))) {

            // only one track at a time allowed
            if (audio != null) {
                audio.close();
                audio = null;
            }

            // if from script, local to that path
            URL audioFile;
            ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
            if (stapp.getScripts().isPlaying()) {
                audioFile = locatorUtil.getOrLoadFile(stapp.getScripts().getScriptPath() + "/" + args.get("filename"));
            } else {
                audioFile = locatorUtil.getDataFile(args.get("filename"));
            }

            System.out.println("audio path = " + audioFile);

            audio = new Audio(audioFile.getPath(), "default track", (long) strToDouble(args.get("output_rate")));
            String loopValue = args.get("loop");
            audio.play(!isEmpty(loopValue) && loopValue.equals("on"));

            // if fast forwarding mute (pause) audio
            if (stapp.getTimeMultiplier() != 1) {
                audio.pause();
            }
        } else if (!isEmpty(args.get("volume"))) {

            recordable = false;
            if (audio != null) {
                String volumeValue = args.get("volume");
                if ("increment".equals(volumeValue)) {
                    audio.incrementVolume();
                } else if ("decrement".equals(volumeValue)) {
                    audio.decrementVolume();
                } else audio.setVolume((float) strToDouble(volumeValue));
            }
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
        return recordable;
    }

    private void setImageCommand(String commandline, Map<String, String> args, boolean status) {
        ImageMgr scriptImages = getCore().getImageMgr();
        if (isEmpty(args.get("name"))) {
            throw new StellariumCommandException(commandline, "Image name required");
        } else if (args.get("action").equals("drop")) {
            scriptImages.dropImage(args.get("name"));
        } else {
            if (args.get("action").equals("load") && !isEmpty(args.get("filename"))) {
                Image.IMAGE_POSITIONING imgPos = Image.IMAGE_POSITIONING.POS_VIEWPORT;
                String coordinateSystem = args.get("coordinate_system");
                if ("horizontal".equals(coordinateSystem)) imgPos = Image.IMAGE_POSITIONING.POS_HORIZONTAL;
                else if ("equatorial".equals(coordinateSystem)) imgPos = Image.IMAGE_POSITIONING.POS_EQUATORIAL;
                else if ("j2000".equals(coordinateSystem)) imgPos = Image.IMAGE_POSITIONING.POS_J2000;

                String imageFileName;
                ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
                if (stapp.getScripts().isPlaying()) {
                    imageFileName = locatorUtil.getOrLoadFile(stapp.getScripts().getScriptPath() + args.get("filename")).getPath();
                } else {
                    imageFileName = locatorUtil.getDataFile(args.get("filename")).getPath();
                }
                status = scriptImages.loadImage(imageFileName, args.get("name"), imgPos);
                if (!status) {
                    throw new StellariumCommandException(commandline, "Unable to open file: " + imageFileName);
                }
            }

            if (status) {
                Image img = scriptImages.getImage(args.get("name"));

                if (img != null) {
                    if (!isEmpty(args.get("alpha")))
                        img.setAlpha((float) strToDouble(args.get("alpha")),
                                (float) strToDouble(args.get("duration")));
                    if (!isEmpty(args.get("scale")))
                        img.setScale((float) strToDouble(args.get("scale")),
                                (float) strToDouble(args.get("duration")));
                    if (!isEmpty(args.get("rotation")))
                        img.setRotation((float) strToDouble(args.get("rotation")),
                                (float) strToDouble(args.get("duration")));
                    if (!isEmpty(args.get("xpos")) || !isEmpty(args.get("ypos")))
                        img.setLocation((float) strToDouble(args.get("xpos")),
                                !isEmpty(args.get("xpos")),
                                (float) strToDouble(args.get("ypos")),
                                !isEmpty(args.get("ypos")),
                                (float) strToDouble(args.get("duration")));
                    // for more human readable scripts, as long as someone doesn't do both...
                    if (!isEmpty(args.get("altitude")) || !isEmpty(args.get("azimuth")))
                        img.setLocation((float) strToDouble(args.get("altitude")),
                                !isEmpty(args.get("altitude")),
                                (float) strToDouble(args.get("azimuth")),
                                !isEmpty(args.get("azimuth")),
                                (float) strToDouble(args.get("duration")));
                } else {
                    throw new StellariumCommandException(commandline, "Unable to find image: " + args.get("name"));
                }
            }
        }
    }

    private void setMoveToCommand(String commandline, Map<String, String> args, String command) {
        if (!isEmpty(args.get("lat")) || !isEmpty(args.get("lon")) || !isEmpty(args.get("alt"))) {
            Observator observatory = getCore().getObservatory();

            double lat = observatory.getLatitude();
            double lon = observatory.getLongitude();
            double alt = observatory.getAltitude();
            String name = null;
            int delay;

            if (!isEmpty(args.get("name"))) name = args.get("name");
            if (!isEmpty(args.get("lat"))) lat = strToDouble(args.get("lat"));
            if (!isEmpty(args.get("lon"))) lon = strToDouble(args.get("lon"));
            if (!isEmpty(args.get("alt"))) alt = strToDouble(args.get("alt"));
            delay = (int) (1000. * strToDouble(args.get("duration")));

            getCore().moveObserver(lat, lon, alt, delay, name);
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
    }

    private void setDateCommand(String commandline, Map<String, String> args, String command) {
        // ISO 8601-like format [+/-]YYYY-MM-DDThh:mm:ss (no timzone offset, T is literal)
        if (!isEmpty(args.get("local"))) {
            double jd;
            String newDate;
            String localDate = args.get("local");
            if (localDate.charAt(0) == 'T') {
                // set time only (don't change day)
                String skyDate = stapp.getISO8601TimeLocal(getCore().getJulianDay());
                newDate = skyDate.substring(0, 10) + localDate;
            } else {
                newDate = localDate;
            }

            jd = stringToJDay(newDate);
            getCore().setJDay(jd - stapp.getGMTShift(jd) * Navigator.JD_HOUR);
        } else if (!isEmpty(args.get("utc"))) {
            double jd = stringToJDay(args.get("utc"));
            getCore().setJDay(jd);
        } else if (!isEmpty(args.get("relative"))) {// value is a double number of days
            double days = strToDouble(args.get("relative"));
            getCore().setJDay(getCore().getJulianDay() + days);
        } else if ("current".equals(args.get("load"))) {
            // set date to current date
            getCore().setJDay(JulianDay.getJulianFromSys());
        } else if ("preset".equals(args.get("load"))) {
            // set date to preset (or current) date, based on user setup
            // TODO: should this record as the actual date used?
            if (stapp.getStartupTimeMode().equalsIgnoreCase("preset"))
                getCore().setJDay(stapp.getPresetSkyTime() -
                        stapp.getGMTShift(stapp.getPresetSkyTime()) * NavigatorIfc.JD_HOUR);
            else getCore().setJDay(JulianDay.getJulianFromSys());

        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
    }

    private String setTimerateCommand(String commandline, Map<String, String> args, String command) {
        if (!isEmpty(args.get("rate"))) {
            stapp.setTimeSpeed(strToDouble(args.get("rate")) * Navigator.JD_SECOND);
        } else if ("pause".equals(args.get("action"))) {
            stapp.setFlagTimePause(!stapp.isFlagTimePause());
            if (stapp.isFlagTimePause()) {
                stapp.setTempTimeVelocity(getCore().getTimeSpeed());
                stapp.setTimeSpeed(0);
            } else {
                stapp.setTimeSpeed(stapp.getTempTimeVelocity());
            }
        } else if ("resume".equals(args.get("action"))) {
            stapp.setFlagTimePause(false);
            stapp.setTimeSpeed(stapp.getTempTimeVelocity());
        } else if ("increment".equals(args.get("action"))) {
            // speed up time rate
            double s = getCore().getTimeSpeed();
            if (s >= Navigator.JD_SECOND) {
                s *= 10;
            } else if (s < -Navigator.JD_SECOND) {
                s /= 10;
            } else if (s >= 0 && s < Navigator.JD_SECOND) {
                s = Navigator.JD_SECOND;
            } else if (s >= -Navigator.JD_SECOND && s < 0.) {
                s = 0;
            }
            stapp.setTimeSpeed(s);

            // for safest script replay, record as absolute amount
            commandline = "timerate rate " + doubleToStr(s / Navigator.JD_SECOND);
        } else if ("decrement".equals(args.get("action"))) {
            double s = getCore().getTimeSpeed();
            if (s > Navigator.JD_SECOND) {
                s /= 10;
            } else if (s <= -Navigator.JD_SECOND) {
                s *= 10;
            } else if (s > -Navigator.JD_SECOND && s <= 0.) {
                s = -Navigator.JD_SECOND;
            } else if (s > 0. && s <= Navigator.JD_SECOND) {
                s = 0;
            }
            getCore().setTimeSpeed(s);

            // for safest script replay, record as absolute amount
            commandline = "timerate rate " + doubleToStr(s / Navigator.JD_SECOND);
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
        return commandline;
    }

    private void setZoomCommand(String commandline, Map<String, String> args, String command) {
        float duration = (float) Math.abs(strToDouble(args.get("duration")));

        if (!isEmpty(args.get("auto"))) {
            // auto zoom using specified or default duration
            if (isEmpty(args.get("duration"))) {
                duration = getCore().getAutoMoveDuration();
            }

            if (args.get("auto").equals("out")) {
                getCore().autoZoomOut(duration, false);
            } else if (args.get("auto").equals("initial")) {
                getCore().autoZoomOut(duration, true);
            } else if (args.get("manual").equals("1")) {
                getCore().autoZoomIn(duration, true);// have to explicity allow possible manual zoom
            } else {
                getCore().autoZoomIn(duration, false);
            }
        } else if (!isEmpty(args.get("fov"))) {
            // zoom to specific field of view
            getCore().zoomTo(strToDouble(args.get("fov")), strToDouble(args.get("duration")));
        } else if (!isEmpty(args.get("delta_fov"))) {
            getCore().setFov(getCore().getFieldOfView() + strToDouble(args.get("delta_fov")));
        } else { // should we record absolute fov instead of delta? isn't usually smooth playback
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }
    }

    private void setLookCommand(Map<String, String> args) {
        // change direction of view
        //	  double duration = str_to_pos_double(args["duration"]);

        if (!isEmpty(args.get("delta_az")) || !isEmpty(args.get("delta_alt"))) {
            // immediately change viewing direction
            getCore().panView(strToDouble(args.get("delta_az")), strToDouble(args.get("delta_alt")));
            //                    } else {
            //                        status = 0;
        }

        // TODO absolute settings (see RFE 1311031)
    }

    private void setSelectCommand(Map<String, String> args) {
        // default is to deselect current object
        getCore().unSelect();

        String selectType = "", identifier = null;
        final String[] SUPPORTED_ARG = {"hp", "star", "planet", "nebula", "constellation", "constellation_star"};
        int i;
        for (i = 0; i < SUPPORTED_ARG.length; i++) {
            selectType = SUPPORTED_ARG[i];
            identifier = args.get(selectType);
            if (!isEmpty(identifier)) {
                break;
            }
        }

        if (!isEmpty(identifier)) {
            getCore().selectObject(selectType, identifier);
        }

        // determine if selected object pointer should be displayed
        String pointerArgs = args.get("pointer");
        if (pointerArgs.equals("off") || pointerArgs.equals("0")) {
            getCore().setObjectPointer(false);
        } else {
            getCore().setObjectPointer(true);
        }
    }

    private void setSetCommand(String commandline, boolean trusted, Map<String, String> args, String command) {
        // set core variables

        // TODO: some bounds/error checking here

        if (!isEmpty(args.get("atmosphere_fade_duration"))) {
            getCore().setAtmosphereFadeDuration((float) strToDouble(args.get("atmosphere_fade_duration")));
        } else if (!isEmpty(args.get("auto_move_duration"))) {
            getCore().setAutoMoveDuration((float) strToDouble(args.get("auto_move_duration")));
        } else if (!isEmpty(args.get("constellation_art_fade_duration"))) {
            getCore().setConstellationArtFadeDuration((float) strToDouble(args.get("constellation_art_fade_duration")));
        } else if (!isEmpty(args.get("constellation_art_intensity"))) {
            getCore().setConstellationArtIntensity((float) strToDouble(args.get("constellation_art_intensity")));
        } else if (!isEmpty(args.get("home_planet"))) {
            getCore().setHomePlanet(args.get("home_planet"));
        } else if (!isEmpty(args.get("landscape_name"))) {
            getCore().setLandscape(args.get("landscape_name"));
        } else if (!isEmpty(args.get("max_mag_nebula_name"))) {
            getCore().setNebulaMaxMagHints((float) strToDouble(args.get("max_mag_nebula_name")));
        } else if (!isEmpty(args.get("max_mag_star_name"))) {
            getCore().setMaxMagStarName((float) strToDouble(args.get("max_mag_star_name")));
        } else if (!isEmpty(args.get("moon_scale"))) {
            getCore().setMoonScale((float) strToDouble(args.get("moon_scale")));
        } else if (!isEmpty(args.get("sky_culture"))) {
            getCore().setSkyCulture(args.get("sky_culture"));
        } else if (!isEmpty(args.get("sky_locale"))) {
            getCore().setSkyLocale(Translator.codeToLocale(args.get("sky_locale")));
        } else if (!isEmpty(args.get("star_mag_scale"))) {
            getCore().setStarMagScale((float) strToDouble(args.get("star_mag_scale")));
        } else if (!isEmpty(args.get("star_scale"))) {
            float scale = (float) strToDouble(args.get("star_scale"));
            getCore().setStarScale(scale);
            getCore().setPlanetsScale(scale);
        } else if (!isEmpty(args.get("nebula_scale"))) {
            float scale = (float) strToDouble(args.get("nebula_scale"));
            getCore().setNebulaCircleScale(scale);
        } else if (!isEmpty(args.get("star_twinkle_amount"))) {
            getCore().setStarTwinkleAmount((float) strToDouble(args.get("star_twinkle_amount")));
        } else if (!isEmpty(args.get("time_zone"))) {
            stapp.setCustomTimezone(args.get("time_zone"));
        } else if (!isEmpty(args.get("milky_way_intensity"))) {
            getCore().setMilkyWayIntensity((float) strToDouble(args.get("milky_way_intensity")));
            // safety feature to be able to turn back on
            if (getCore().getMilkyWayIntensity() != 0) {
                getCore().setMilkyWay(true);
            }
        } else {
            throw new StellariumCommandException(commandline, "Unrecognized arguments to " + command + ": " + args.toString());
        }

        if (trusted) {
            //    else if(args[IniFileParser.BASE_FONT_SIZE]!="") stCore.BaseFontSize = strToDouble(args[IniFileParser.BASE_FONT_SIZE]);
            //	else if(args["bbp_mode"]!="") stCore.BbpMode = strToDouble(args["bbp_mode"]);
            //    else if(args["date_display_format"]!="") stCore.DateDisplayFormat = args["date_display_format"];
            //	else if(args[IniFileParser.FULLSCREEN]!="") stCore.Fullscreen = args[IniFileParser.FULLSCREEN];
            //	else if(args["horizontal_offset"]!="") stCore.HorizontalOffset = strToDouble(args["horizontal_offset"]);
            //	else if(args["init_fov"]!="") stCore.initFov = strToDouble(args["init_fov"]);
            //	else if(args["preset_sky_time"]!="") stCore.presetSkyTime = strToDouble(args["preset_sky_time"]);
            //	else if(args[IniFileParser.SCREEN_HEIGHT]!="") stCore.ScreenH = strToDouble(args[IniFileParser.SCREEN_HEIGHT]);
            //	else if(args[IniFileParser.SCREEN_WIDTH]!="") stCore.ScreenW = strToDouble(args[IniFileParser.SCREEN_WIDTH]);
            //    else if(args["startup_time_mode"]!="") stCore.startupTimeMode = args["startup_time_mode"];
            // else if(args["time_display_format"]!="") stCore.TimeDisplayFormat = args["time_display_format"];
            //else if(args["Type"]!="") stCore.Type = args["Type"];
            //else if(args["version"]!="") stCore.Version = strToDouble(args["version"]);
            //      else if(args["vertical_offset"]!="") stCore.VerticalOffset = strToDouble(args["vertical_offset"]);
            //else if(args["viewing_mode"]!="") stCore.viewingMode = args["viewing_mode"];
            //else if(args[IniFileParser.VIEWPORT]!="") stCore.Viewport = args[IniFileParser.VIEWPORT];
        }
    }

    private long setWaitCommand(long wait, Map<String, String> args) {
        double fdelay = strToDouble(args.get("duration"));
        if (fdelay > 0) {
            wait = (int) (fdelay * 1000);
        }
        return wait;
    }

    private String setFlagCommand(String commandline, boolean trusted, Map<String, String> args, String command) {
        // could loop if want to allow that syntax
        if (args.size() == 1) {
            Map.Entry<String, String> begin = args.entrySet().iterator().next();
            boolean newval = setFlag(begin.getKey(), begin.getValue(), trusted);

            // rewrite command for recording so that actual state is known (rather than "toggle")
            if ("toggle".equals(begin.getValue())) {
                commandline = command + " " + begin.getKey() + " " + newval;
            }
        } else {
            throw new StellariumCommandException(commandline, "New flag value expected");
        }
        return commandline;
    }

    /**
     * set flags
     * if caller is not trusted, some flags can't be changed
     * newval is new value of flag changed
     *
     * @param name
     * @param value
     * @param trusted
     * @return
     */
    private boolean setFlag(String name, String value, boolean trusted) {
        boolean newval = false;

        // value can be "on", "off", or "toggle"
        if (value.equals("toggle")) {
            if (trusted) {
                /* disabled due to code rework

                // normal scripts shouldn't be able to change these user settings
                if(name=="enable_zoom_keys") {
                    newval = !stcore->getFlagEnableZoomKeys();
                    stcore->setFlagEnableZoomKeys(newval); }
                else if(name=="enable_move_keys") {
                    newval = !stcore->getFlagEnableMoveKeys();
                    stcore->setFlagEnableMoveKeys(newval); }
                else if(name=="enable_move_mouse") newval = (stapp->FlagEnableMoveMouse = !stapp->FlagEnableMoveMouse);
                else if(name=="menu") newval = (stapp->ui->FlagMenu = !stapp->ui->FlagMenu);
                else if(name=="help") newval = (stapp->ui->FlagHelp = !stapp->ui->FlagHelp);
                else if(name=="infos") newval = (stapp->ui->FlagInfos = !stapp->ui->FlagInfos);
                else if(name=="show_topbar") newval = (stapp->ui->FlagShowTopBar = !stapp->ui->FlagShowTopBar);
                else if(name=="show_time") newval = (stapp->ui->FlagShowTime = !stapp->ui->FlagShowTime);
                else if(name=="show_date") newval = (stapp->ui->FlagShowDate = !stapp->ui->FlagShowDate);
                else if(name=="show_appname") newval = (stapp->ui->FlagShowAppName = !stapp->ui->FlagShowAppName);
                else if(name=="show_fps") newval = (stapp->ui->FlagShowFps = !stapp->ui->FlagShowFps);
                else if(name=="show_fov") newval = (stapp->ui->FlagShowFov = !stapp->ui->FlagShowFov);
                else if(name=="enable_tui_menu") newval = (stapp->ui->FlagEnableTuiMenu = !stapp->ui->FlagEnableTuiMenu);
                else if(name=="show_gravity_ui") newval = (stapp->ui->FlagShowGravityUi = !stapp->ui->FlagShowGravityUi);
                else if(name=="gravity_labels") {
                    newval = !stcore->getFlagGravityLabels();
                    stcore->setFlagGravityLabels(newval);
                    }
                else status = 0;  // no match here, anyway
                */
            } else {
                throw new StellariumCommandException(name + "=" + value, "unstrusted");
            }

            if (name.equals("constellation_drawing")) {
                newval = !getCore().isConstellationLinesEnabled();
                getCore().setFlagConstellationLines(newval);
            } else if (name.equals("constellation_names")) {
                newval = !getCore().getFlagConstellationNames();
                getCore().setFlagConstellationNames(newval);
            } else if (name.equals("constellation_art")) {
                newval = !getCore().getFlagConstellationArt();
                getCore().setFlagConstellationArt(newval);
            } else if (name.equals("constellation_boundaries")) {
                newval = !getCore().getFlagConstellationBoundaries();
                getCore().setFlagConstellationBoundaries(newval);
            } else if (name.equals("constellation_pick")) {
                newval = !getCore().getFlagConstellationIsolateSelected();
                getCore().setFlagConstellationIsolateSelected(newval);
            } else if (name.equals("star_twinkle")) {
                newval = !getCore().isStarTwinkleEnabled();
                getCore().setStarTwinkle(newval);
            } else if (name.equals("point_star")) {
                newval = !getCore().getPointStar();
                getCore().setPointStar(newval);
            } else if (name.equals("show_selected_object_info")) {
                newval = !stapp.getUi().isShowSelectedObjectInfo();
                stapp.getUi().setShowSelectedObjectInfo(newval);
            } else if (name.equals("show_tui_datetime")) {
                newval = !stapp.getUi().getTui().isShowTuiDateTime();
                stapp.getUi().getTui().setShowTuiDateTime(newval);
            } else if (name.equals("show_tui_short_obj_info")) {
                newval = !stapp.getUi().getTui().isShowTuiShortObjInfo();
                stapp.getUi().getTui().setShowTuiShortObjInfo(newval);
            } else if (name.equals("manual_zoom")) {
                newval = !getCore().getFlagManualAutoZoom();
                getCore().setFlagManualAutoZoom(newval);
            } else if (name.equals("show_script_bar")) {
                newval = !stapp.getUi().isShowScriptBar();
                stapp.getUi().setShowScriptBar(newval);
            } else if (name.equals("fog")) {
                newval = !getCore().isFogEnabled();
                getCore().setFlagFog(newval);
            } else if (name.equals("atmosphere")) {
                newval = !getCore().isAtmosphereEnabled();
                getCore().setAtmosphere(newval);
                if (!newval) getCore().setFlagFog(false);// turn off fog with atmosphere
            }
            /*		else if(name.equals("chart")) {
                newval = !stapp.getVisionModeChart();
                if (newval) stapp.setVisionModeChart();
            }
            else if(name.equals("night")) {
                newval = !stapp.getVisionModeNight();
                if (newval) stapp.setVisionModeNight();
            }
            */
            //else if(name.equals("use_common_names")) newval = (stcore.FlagUseCommonNames = !stcore.FlagUseCommonNames);
            else if (name.equals("azimuthal_grid")) {
                newval = !getCore().isAzimutalGridEnabled();
                getCore().setAzimutalGrid(newval);
            } else if (name.equals("equatorial_grid")) {
                newval = !getCore().isEquatorGridEnabled();
                getCore().setEquatorGrid(newval);
            } else if (name.equals("equator_line")) {
                newval = !getCore().isEquatorLineEnabled();
                getCore().setEquatorLine(newval);
            } else if (name.equals("ecliptic_line")) {
                newval = !getCore().isEclipticLineEnabled();
                getCore().setEclipticLine(newval);
            } else if (name.equals("meridian_line")) {
                newval = !getCore().getFlagMeridianLine();
                getCore().setMeridianLine(newval);
            } else if (name.equals("cardinal_points")) {
                newval = !getCore().isCardinalsPointsEnabled();
                getCore().setCardinalsPointsEnabled(newval);
            } else if (name.equals("moon_scaled")) {
                newval = !getCore().isMoonScaled();
                getCore().setMoonScaled(newval);
            } else if (name.equals("landscape")) {
                newval = !getCore().isLandscapeEnabled();
                getCore().setLandscapeEnabled(newval);
            } else if (name.equals("stars")) {
                newval = !getCore().isStarEnabled();
                getCore().setStars(newval);
            } else if (name.equals("star_names")) {
                newval = !getCore().isStarNameEnabled();
                getCore().setStarNames(newval);
            } else if (name.equals("planets")) {
                newval = !getCore().isPlanetsEnabled();
                getCore().setPlanets(newval);
                if (!getCore().isPlanetsEnabled()) getCore().setPlanetsHints(false);
            } else if (name.equals("planet_names")) {
                newval = !getCore().isPlanetsHintsEnabled();
                getCore().setPlanetsHints(newval);
                if (getCore().isPlanetsHintsEnabled())
                    getCore().setPlanets(true);// for safety if script turns planets off
            } else if (name.equals("planet_orbits")) {
                newval = !getCore().getFlagPlanetsOrbits();
                getCore().setPlanetsOrbits(newval);
            } else if (name.equals("nebulae")) {
                newval = !getCore().getFlagNebula();
                getCore().setNebula(newval);
            } else if (name.equals("nebula_names")) {
                newval = !getCore().isNebulaHintEnabled();
                if (newval) getCore().setNebula(true);// make sure visible
                getCore().setNebulaHints(newval);
            } else if (name.equals("milky_way")) {
                newval = !getCore().getFlagMilkyWay();
                getCore().setMilkyWay(newval);
            } else if (name.equals("bright_nebulae")) {
                newval = !getCore().getFlagBrightNebulae();
                getCore().setBrightNebulae(newval);
            } else if (name.equals("object_trails")) {
                newval = !getCore().getFlagPlanetsTrails();
                getCore().setPlanetsTrails(newval);
            } else if (name.equals("track_object")) {
                newval = !getCore().getFlagTracking();
                getCore().setTracking(newval);
            } else if (name.equals("script_gui_debug")) {// Not written to config - script specific
                newval = !stapp.getScripts().isGuiDebug();
                stapp.getScripts().setGuiDebug(newval);
            } else {
                return newval;   // no matching flag found untrusted, but maybe trusted matched
            }
        } else {
            newval = (value.equalsIgnoreCase("on") || value.equals("1"));

            if (trusted) {
                /* disabled due to code rework
                // normal scripts shouldn't be able to change these user settings
                if(name=="enable_zoom_keys") stcore->setFlagEnableZoomKeys(newval);
                else if(name=="enable_move_keys") stcore->setFlagEnableMoveKeys(newval);
                else if(name=="enable_move_mouse") stapp->FlagEnableMoveMouse = newval;
                else if(name=="menu") stapp->ui->FlagMenu = newval;
                else if(name=="help") stapp->ui->FlagHelp = newval;
                else if(name=="infos") stapp->ui->FlagInfos = newval;
                else if(name=="show_topbar") stapp->ui->FlagShowTopBar = newval;
                else if(name=="show_time") stapp->ui->FlagShowTime = newval;
                else if(name=="show_date") stapp->ui->FlagShowDate = newval;
                else if(name=="show_appname") stapp->ui->FlagShowAppName = newval;
                else if(name=="show_fps") stapp->ui->FlagShowFps = newval;
                else if(name=="show_fov") stapp->ui->FlagShowFov = newval;
                else if(name=="enable_tui_menu") stapp->ui->FlagEnableTuiMenu = newval;
                else if(name=="show_gravity_ui") stapp->ui->FlagShowGravityUi = newval;
                else if(name=="gravity_labels") stcore->setFlagGravityLabels(newval);
                else status = 0;
                */
            } else {
                throw new StellariumCommandException(name + "=" + value, "unstrusted");
            }

            if (name.equals("constellation_drawing")) getCore().setFlagConstellationLines(newval);
            else if (name.equals("constellation_names")) getCore().setFlagConstellationNames(newval);
            else if (name.equals("constellation_art")) getCore().setFlagConstellationArt(newval);
            else if (name.equals("constellation_boundaries")) getCore().setFlagConstellationBoundaries(newval);
            else if (name.equals("constellation_pick")) getCore().setFlagConstellationIsolateSelected(newval);
            else if (name.equals("star_twinkle")) getCore().setStarTwinkle(newval);
            else if (name.equals("point_star")) getCore().setPointStar(newval);
            else if (name.equals("show_selected_object_info")) stapp.getUi().setShowSelectedObjectInfo(newval);
            else if (name.equals("show_tui_datetime")) stapp.getUi().getTui().setShowTuiDateTime(newval);
            else if (name.equals("show_tui_short_obj_info")) stapp.getUi().getTui().setShowTuiShortObjInfo(newval);
            else if (name.equals("manual_zoom")) getCore().setFlagManualAutoZoom(newval);
            else if (name.equals("show_script_bar")) stapp.getUi().setShowScriptBar(newval);
            else if (name.equals("fog")) getCore().setFlagFog(newval);
            else if (name.equals("atmosphere")) {
                getCore().setAtmosphere(newval);
                if (!newval) getCore().setFlagFog(false);// turn off fog with atmosphere
            }
            /*		else if(name=="chart")) {
                if (newval) stapp->setVisionModeChart();
            }
            else if(name=="night")) {
                if (newval) stapp->setVisionModeNight();
            }
            */
            else if (name.equals("azimuthal_grid")) {
                getCore().setAzimutalGrid(newval);
            } else if (name.equals("equatorial_grid")) getCore().setEquatorGrid(newval);
            else if (name.equals("equator_line")) getCore().setEquatorLine(newval);
            else if (name.equals("ecliptic_line")) getCore().setEclipticLine(newval);
            else if (name.equals("meridian_line")) getCore().setMeridianLine(newval);
            else if (name.equals("cardinal_points")) getCore().setCardinalsPointsEnabled(newval);
            else if (name.equals("moon_scaled")) getCore().setMoonScaled(newval);
            else if (name.equals("landscape")) getCore().setLandscapeEnabled(newval);
            else if (name.equals("stars")) getCore().setStars(newval);
            else if (name.equals("star_names")) getCore().setStarNames(newval);
            else if (name.equals("planets")) {
                getCore().setPlanets(newval);
                if (!getCore().isPlanetsEnabled()) getCore().setPlanetsHints(false);
            } else if (name.equals("planet_names")) {
                getCore().setPlanetsHints(newval);
                if (getCore().isPlanetsHintsEnabled())
                    getCore().setPlanets(true);// for safety if script turns planets off
            } else if (name.equals("planet_orbits")) getCore().setPlanetsOrbits(newval);
            else if (name.equals("nebulae")) getCore().setNebula(newval);
            else if (name.equals("nebula_names")) {
                getCore().setNebula(true);// make sure visible
                getCore().setNebulaHints(newval);
            } else if (name.equals("milky_way")) getCore().setMilkyWay(newval);
            else if (name.equals("bright_nebulae")) getCore().setBrightNebulae(newval);
            else if (name.equals("object_trails")) getCore().setPlanetsTrails(newval);
            else if (name.equals("track_object")) getCore().setTracking(newval);
            else if (name.equals("script_gui_debug")) {
                stapp.getScripts().setGuiDebug(newval);// Not written to config - script specific
            }
        }

        return newval;
    }

    private StelCore getCore() {
        return stapp.getCore();
    }


    public void update(int deltaTime) {
        if (audio != null) {
            audio.update(deltaTime);
        }
    }

    double strToDouble(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        str = str.replace(',', '.');    // TODO(JBE): Use localized NumberFormat ?
        return Double.parseDouble(str);
    }

    int strToInt(String str) {
        if (isEmpty(str))
            return 0;
        return java.lang.Integer.parseInt(str);
    }

    String doubleToStr(double dbl) {
        return Double.toString(dbl);
    }

    /**
     * The commanded application
     */
    private StelApp stapp;

    /**
     * for audio track from script
     */
    private Audio audio;
}