/**
* Copyright 2014-2020 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package com.ibm.datapower.er;

import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import java.util.EventObject;
import java.util.EventListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class CommandLineArgs implements a command line argument pre-processor.  It
 * is typically used to process arguments passed from the command line to the
 * main () method of an application.  It has features that allow you to control
 * the prefix character used for the argument switches as well as listening for
 * arguments in the command string.  It also includes a nice feature for turning
 * the command string into a properties file and then using this properties file
 * as the future command string.  To use this feature, add to the command line
 * string the "-@save <filename>" directive or "-@restore <filename>" directive,
 * to save or restore a properties file respectively.
 *
 * @author Brien Muschett
 * @version 1.0
 * @see com.ibm.vxi.utils.CommandLineArgs.CommandLineListener
 * @see com.ibm.vxi.utils.CommandLineArgs.CommandLineEvent
 */
public class ERCommandLineArgs extends Properties {

  /**
	 * 
	 */
	private static final long serialVersionUID = 5880684163799584433L;
/**
   * Construct an initially empty command line.  Useful for setting the state
   * and then passing the arguments.
   *
   */
  public ERCommandLineArgs () {

    this (DEFAULT_ARGS);

  } // CommandLineArgs ()


  /**
   * Construct a Command Line using the given arguments.  Use the following defaults:
   * DEFAULT_SWITCH_PREFIX, DEFAULT_SAVE_SWITCH, DEFAULT_RESTORE_SWITCH.
   *
   * @param args - The list of command line arguments to process.
   */
  public ERCommandLineArgs (String [] args) {

    super ();
    support = new Vector ();
    setSwitchPrefix (DEFAULT_SWITCH_PREFIX);
    setSaveSwitch (DEFAULT_SAVE_SWITCH);
    setRestoreSwitch (DEFAULT_RESTORE_SWITCH);
    setUsageText (null);
    setArgs (args);

  } // CommandLineArgs ()

  /**
   * Method to set the command line arguments.  If there are any prexisting arguments
   * they will be deleted and the new arguments parsed.
   *
   * @param args - The list of command line arguments to process.
   */
  public void setArgs (String [] args) {

    this.args = args;

  } // setArgs ()


  /**
   * Method to set the prefix characters for any switches contained in the
   * command line arguments.  The switch prefix is typically the hypen "-".
   *
   * @param switchPrefix - string representing the prefix to use for switches.
   */
  public void setSwitchPrefix (String switchPrefix) {

    this.switchPrefix = switchPrefix;

  } // setSwitchPrefix ()


  /**
   * Method to set the switch for saving the command line arguments into a
   * properties file.  The default switch for doing this is "-@save <filename>".
   *
   * @param saveSwitch - The switch to use for saving the arguments.
   */
  public void setSaveSwitch (String saveSwitch) {

    this.saveSwitch = saveSwitch;

  } // setSaveSwitch ()


  /**
   * Method to set the switch for restoring the command line arguments from a
   * properties file.  The default switch for doing this is "-@restore <filename>".
   *
   * @param restoreSwitch - The switch to use for restoring the arguments.
   */
  public void setRestoreSwitch (String restoreSwitch) {

    this.restoreSwitch = restoreSwitch;

  } // setRestoreSwitch ()


  /**
   * Method to set the usage statement for this Command Line.  The usage text
   * is displayed if the command line does not contain any arguments or the
   * arguments cannot be parsed correctly.  To disable, set the text to null.
   *
   * @param usageText - The statement to display if commands are missing.
   */
  public void setUsageText (String usageText) {

    this.usageText = usageText;

  } // setUsageText ()


  /**
   * Method to retrieve the usage text.
   *
   * @return The usage statement.
   */
  public String getUsageText () {

    return usageText;

  } // setUsageText ()


  /**
   * Method to retrieve the command line arguments.
   *
   * @return The command line arguments.
   */
  public String [] getArgs () {

    return args;

  } // getArgs ()


  /**
   * Method to retrieve the switch prefix.
   *
   * @return The prefix.
   */
  public String getSwitchPrefix () {

    return switchPrefix;

  } // getSwitchPrefix ()


  /**
   * Method to retrieve the save switch.
   *
   * @return The save switch.
   */
  public String getSaveSwitch () {

    return saveSwitch;

  } // getSaveSwitch ()


  /**
   * Method to retrieve the restore switch.
   *
   * @return The restore switch.
   */
  public String getRestoreSwitch () {

    return restoreSwitch;

  } // getRestoreSwitch ()


  /**
   * Method to add a listener to the list of objects to be notified when
   * a command line argument is parsed.
   *
   * @param l - The object interested in listening for events.
   */
  public void addCommandLineListener (CommandLineListener l) {

    support.addElement (l);

  } // addCommandLineListener ()


  /**
   * Method to remove a listener from the list of objects to be notified when
   * a command line argument is parsed.
   *
   * @param l - The object interested in being removed from the list.
   */
  public void removeCommandLineListener (CommandLineListener l) {

    support.removeElement (l);

  } // removeCommandLineListener ()


  /**
   * Method called to parse the argument list.  This typically ocurrs immediately
   * after the setArgs () call is made.
   */
  protected void parseArgs  () {

	  if ( args == null )
		  return;
	  
    int argIndex = 0;
    int argCount = args.length;
    String argKey = null;
    String argValue = null;


    while (argIndex < argCount) {

      argKey = args [argIndex++];
      argValue = (argIndex < argCount) ? args [argIndex] : "";

      /*
      **-----------------------------------------------------------------
      ** If the value of the switch is another switch then this is a
      ** unary switch ie. it has no value.
      **-----------------------------------------------------------------
      */
      if (argValue.startsWith (switchPrefix))  // another switch?
        argValue = "";                         // no value
      else                                     // switch has a value
        argIndex++;                            // increment argument

      if (argKey != null)
        put (argKey, argValue);

    } // end while

  } // parseArgs ()


  /**
   * Method called to save and restore the arguments to and from property files.
   * This is typcially called after parseArgs () is called.
   */
  protected void saveAndRestore () {

    String saveFileName = (String) get (saveSwitch);
    String restoreFileName = (String) get (restoreSwitch);

    // remove the keys so no events are fired on them
    remove (saveSwitch);
    remove (restoreSwitch);


    if (saveFileName != null) {

      FileOutputStream fos = null;

      try {

        fos = new FileOutputStream (saveFileName);
  // OTI JVM deprecated
  // deprecated api in 1.2. Use save () in 1.1
        // (jdk 1.2) store (fos, this.getClass ().getName ());
  // (jdk 1.1) save (fos, this.getClass ().getName ());

      }
      catch (IOException ioe) {
        System.out.println (ioe);
      }
      finally {

        if (fos != null)
          try { fos.close (); } catch (IOException ioe1) { ; }

      }
    }

    if (restoreFileName != null) {

      FileInputStream fis = null;

      try {

        fis = new FileInputStream (restoreFileName);
        load (fis);

      }
      catch (FileNotFoundException fnfe) {
        System.out.println (fnfe);
      }
      catch (IOException ioe) {
        System.out.println (ioe);
      }
      finally {

        if (fis != null)
          try { fis.close (); } catch (IOException ioe) { ; }

      }

    }

  } // saveAndRestore ()


  /**
   * Method to send CommandLineEvent notifications to registered listeners.
   * This method is called after the saveAndRestore () call is made.
   */
  protected void fireCommandLineSupport () {

    String argKey = null;
    String argValue = null;
    CommandLineEvent ce = null;

    /*
    **----------------------------------------------------------------
    ** Go through the list of commands a fire an event to each listener
    ** registered.  We can't do this while processing commands because
    ** they may have been loaded from a properties file.
    **----------------------------------------------------------------
    */

    for (Enumeration e = keys (); e.hasMoreElements ();) {

      argKey = (String) e.nextElement ();
      argValue = (String) get (argKey);
      ce = new CommandLineEvent (this, argKey, argValue);

      for (int i = 0; i < support.size (); i++) {

        ((CommandLineListener) support.elementAt (i)).performCommand (ce);

      }
    }

  } // fireCommandLineSupport


  /**
   * Method to print the usage statement.  This is typically called when
   * no args exist or cannot be parsed correctly.
   */
  protected void printUsageText () {

    if (usageText != null)
      System.out.println (usageText);

  } // printUsageText ()


  public void parse () {

    clear ();                     // cleanup
    parseArgs ();                 // go ahead and start processing
    saveAndRestore ();            // save or restore if requested

    if (size () > 0)              // if we've some commands
      fireCommandLineSupport ();  // update listeners
    else                          // no commands
      printUsageText ();          // print out usage

  } // refresh ()


  /**
   * Interface CommandLineListener represents the callback routine for
   * notification of CommandLineEvents.
   *
   * @author Brien Muschett
   * @version 1.0
   */
  public static interface CommandLineListener extends EventListener {

    public void performCommand (CommandLineEvent cle);

  } // interface CommandLineListener


  /**
   * Class CommandLineEvent represents an event generated when a command
   * line argument is parsed.
   *
   * @author Brien Muschett
   * @version 1.0
   */
  public static class CommandLineEvent extends EventObject {

    public CommandLineEvent (ERCommandLineArgs cla, String arg, String argValue) {

      super (cla);
      this.arg = arg;
      this.argValue = argValue;

    }


    /**
     * Method to get the switch of the event.
     *
     * @return the argument switch.
     */
    public String getSwitch () {

      return arg;

    } // getSwitch ()


    /**
     * Method to get the value of the argument switch.
     *
     * @return the value or empty string if none.
     */
    public String getSwitchValue () {

      return argValue;

    } // getSwitchValue ()


    public String toString () {

      return new String (arg + " = " + argValue);

    } // toString ()


    private String arg;
    private String argValue;

  } // class CommandLineListener


  public static final String DEFAULT_SWITCH_PREFIX = "-";
  public static final String DEFAULT_ARGS [] = { };
  public static final String DEFAULT_RESTORE_SWITCH = DEFAULT_SWITCH_PREFIX + "@restore";
  public static final String DEFAULT_SAVE_SWITCH = DEFAULT_SWITCH_PREFIX + "@save";

  private String switchPrefix;    // the token to use as the switch prefix
  private String args [];         // the command line args passed in
  private String restoreSwitch;   // the switch to use for restoring the arguments
  private String saveSwitch;      // the switch to use for saving the arguments
  private String usageText;       // the usage text to show for no arguments or error
  private Vector support;         // list of listeners for command events
  
}
