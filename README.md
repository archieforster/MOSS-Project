# Flood Infrastructure Resilience Model (FIRM): An Agent-Based Model for Flood Incident Management  
[![An agent based model of flood evacuation](
https://img.youtube.com/vi/o0EOlc5n9O8/0.jpg)](
https://www.youtube.com/watch?v=o0EOlc5n9O8 
"An agent based model of flood evacuation")


## Dependencies

- NetLogo Version 4.1.3 from https://ccl.northwestern.edu/netlogo/4.1.3/
- Extensions in /extensions - these should be moved to your NetLogo installation

## Running from NetLogo GUI

- Open model.nlogo

- Choose the number of vehicles, sea level and starting time (and scenario name if appropriate) for the simulation.

- Click ‘Setup’ to load in the data.

- To start the simulation click ‘Step’.

- To start a flood, click on ‘Destroy Defence’ this will randomly choose a flood defence section and remove it.

- To issue a warning and start an evacuation of individuals driving click ‘Evacuate’.

## Running from Python
Set environment variable NETLOGO with the location of NetLogo.jar. Then:
```
python -m toytown.tests.test_towyn
python -m toytown.tests.test_kibera
python -m toytown.tests.test_bwaise
```
## Acknowledgements
This model was originally developed at Newcastle University by Roger Peppe and Richard Dawson. The research was funded by the Environment Agency (Reliability in Flood Incident Management Planning – SC060063) as part of the Joint DEFRA/EA Flood and Coastal Erosion Risk Management R&D Programme. The overall programme was led by Halcrow but also in collaboration with JBA Consulting, Middlesex University and Bristol University.

The model remains under development, with recent advances involving the incorporation of additional agents (e.g. blue light services) and improved modelling of the floodwave (using the Liang et al. (2007) quadtree approach)

If you are interested you can explore the code for this web enabled version here

Please contact Richard Dawson (richard.dawson@newcastle.ac.uk) for further details.

## References
Bates, P.D. and De Roo, A.P.J. (2000), A simple raster-based model for flood inundation simulation, Journal of Hydrology, 236: 54-77.

Dawson, R. J., Peppe, R. G. and Wang, M. (2011) An agent based model for risk-based flood incident management, Natural Hazards , (doi: 10.1007/s11069-011-9745-4).

Jonkman S.N., Kelman I. (2005) An analysis of causes and circumstances of flood disaster deaths, Disasters, Vol. 29 No. 1 pp. 75-97.

Jonkman, S. N. and Vrijling, J. K. (2008), Loss of life due to floods, J. Flood Risk Management, 1: 43-56.

Liang, Q., Zang, J., Borthwick, A.G.L, Taylor, P.H. (2007) Shallow flow simulation on dynamically adaptive cut cell quadtree grids, International Journal for Numerical Methods in Fluids , 53(12): 1777-1799.
