===FLOOD EVACUATION NETLOGO MODEL===
Requirements:
 - Netlogo 6.4.0
 - Python 3+
 - Fiona package within python (if not present, run 'pip install fiona')

Access to the 'data' subfolder uses relative directories within Netlogo & python code - if this causes errors with data loading, make sure to run the program from within the directory. 
If absolutely necessary, replace local directories with absolute directories in code (these are near the top of the gy-evac.nlogo and path_nav.py files, search for with "./").

==Running==

Within Netlogo running gy-evac.nlogo, use Setup -> reset-ticks -> Go buttons to run the model*

Within path_nav.py, their are various print statements used as part of the debugging process which may better illustrate how the model works. Use replace all "# print" -> "print", and uncomment the main scripts at the bottom of the file to test the navigation in isolation. Note that to run the Netlogo model, both the main script and all print statements must be disabled, otherwise the simulation becomes to slow and crashes.

If in need of any other information or run into any issues please contact s2144728@ed.ac.uk

(*reset-ticks button was used due to weird behaviour where reset-ticks command did not run within setup method.) 
