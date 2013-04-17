////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.webapp.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.denimgroup.threadfix.data.entities.Application;
import com.denimgroup.threadfix.data.entities.ApplicationCriticality;
import com.denimgroup.threadfix.data.entities.Organization;
import com.denimgroup.threadfix.service.OrganizationService;
import com.denimgroup.threadfix.service.SanitizedLogger;

// TODO split into a service? This is all in the controller layer.
@Controller
@RequestMapping("/reports/portfolio")
public class PortfolioReportController {
	
	private final SanitizedLogger log = new SanitizedLogger(PortfolioReportController.class);

	private OrganizationService organizationService;	

	@Autowired
	public PortfolioReportController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * This report is generated by storing the date / criticality information in a 2D integer matrix
	 * and then transforming it into a 2D String matrix with percentages in the controller,
	 * then feeding that into a simple JSP for the view.
	 * 
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String index(Model model, HttpServletRequest request, int teamId) {
		
		Organization org = null;
		String teamName = null;
		
		if (teamId != 0) {
			org = organizationService.loadOrganization(teamId);
			if (org != null && org.getName() != null) {
				teamName = org.getName();
			}
		} 
		
		if (teamName == null) {
			teamName = "All";
		}
		
		Calendar now = Calendar.getInstance();
		List<Organization> teams;
		
		if (org == null) {
			teams = organizationService.loadAllActive();
		} else {
			teams = new ArrayList<Organization>();
			teams.add(org);
		}
		
		int[][] appsByCriticality = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0, 0},
										 {0, 0, 0, 0, 0, 0, 0, 0, 0},
										 {0, 0, 0, 0, 0, 0, 0, 0, 0},
										 {0, 0, 0, 0, 0, 0, 0, 0, 0}};
		
		if (teams == null || teams.isEmpty()) {
			log.warn("No Teams. Redirecting to main page.");
			return "redirect:/";
		}
		log.info("Generating the portfolio-level report.");
		
		Integer numberApplications = 0, numberScans = 0, numberApplicationsNeverScanned = 0;

		// Generate the big table
		List<List<String>> tableContents = new ArrayList<List<String>>();
		List<String> blankRow = null;
		
		List<Boolean> oldArray = new ArrayList<Boolean>();
		
		// each time through generate a team row for the Team and 
		for (Organization team : teams) {
			if (team == null || !team.isActive()) {
				continue;
			}			
			
			List<String> teamRow = null;
			List<List<String>> appRows = new ArrayList<List<String>>();
			
			if (team.getActiveApplications() == null || team.getApplications().isEmpty()) {
				teamRow = Arrays.asList(new String[] { "Team: " + team.getName(), "0", "Never" });
				oldArray.add(true);
			} else {
				Integer totalScans = 0, lowerBound = null, upperBound = 0;
				
				int startCount = oldArray.size();
				oldArray.add(false);
				
				for (Application app : team.getApplications()) {
					if (app == null || !app.isActive())
						continue;
					
					updateCriticalityCounts(appsByCriticality, app, now);
					numberApplications += 1;
					
					String criticality = "";
					if (app.getApplicationCriticality() != null && 
							app.getApplicationCriticality().getName() != null) {
						criticality = app.getApplicationCriticality().getName();
					}
					
					if (app.getScans() == null || app.getScans().isEmpty()) {
						numberApplicationsNeverScanned += 1;
						appRows.add(Arrays.asList(new String[] {app.getName(), criticality, "0", "Never"}));
						oldArray.add(false);
					} else {
						totalScans += app.getScans().size();
						numberScans += app.getScans().size();
						Integer daysSinceLatestScan = getAgeInDays(now, app.getScans().get(0).getImportTime());
						if (lowerBound == null || daysSinceLatestScan < lowerBound) {
							lowerBound = daysSinceLatestScan;
						}
						if (upperBound == null || daysSinceLatestScan > upperBound) {
							upperBound = daysSinceLatestScan;
						}
						appRows.add(Arrays.asList(new String[] {
								app.getName(), 
								criticality, 
								String.valueOf(app.getScans().size()), 
								daysSinceLatestScan.toString()
							}));
						oldArray.add(daysSinceLatestScan > 365);
					}
				}
				
				if (totalScans == 0 || lowerBound == null || upperBound == null) {
					teamRow = Arrays.asList(new String[] { "Team: " + team.getName(),"", "0", "Never" });
				} else {
					if (lowerBound.equals(upperBound)) {
						teamRow = Arrays.asList(new String[] {"Team: " + team.getName(),"", totalScans.toString(), 
								lowerBound.toString()});
					} else {
						teamRow = Arrays.asList(new String[] {"Team: " + team.getName(),"", totalScans.toString(), 
								lowerBound.toString() + "-" + upperBound.toString()});
					}
				}
				
				oldArray.set(startCount, upperBound > 365);
			}
			
			tableContents.add(teamRow);
			tableContents.addAll(appRows);
			tableContents.add(blankRow);
			oldArray.add(false);
		}
		
		if (tableContents.size() == 0) {
			log.warn("No Active Teams. Redirecting to main page.");
			ControllerUtils.addErrorMessage(request, "No active Teams found.");
			return "redirect:/reports";
		}
		
		tableContents.remove(tableContents.size() - 1);
		
		model.addAttribute("totalApps", numberApplications);
		model.addAttribute("totalScans", numberScans);
		model.addAttribute("numberApplicationsNeverScanned", numberApplicationsNeverScanned);
		model.addAttribute("appsByCriticality", createDateTable(appsByCriticality));
		model.addAttribute("tableContents", tableContents);
		model.addAttribute("old",oldArray);
		model.addAttribute("teamName", teamName);
		model.addAttribute("contentPage", "reports/portfolioReport.jsp");
		return "ajaxSuccessHarness";
	}
	
	private String[][] createDateTable(int[][] appsByCriticality) {
		String[] titles = { "Low", "Medium", "High", "Critical"};
		String[][] returnArray = new String[5][10];
		int[] totals = {0,0,0,0,0,0,0,0,0};
		
		for (int i = 0; i < 4; i++) {
			returnArray[i][0] = titles[3-i];
			
			int total = appsByCriticality[3-i][8];
			for (int j = 0; j < 8; j++ ) {
				if (appsByCriticality[3-i][j] != 0) {
					returnArray[i][j+1] = String.valueOf(appsByCriticality[3-i][j]) + 
												" (" + 
												String.valueOf(100*appsByCriticality[3-i][j]/total) +
												"%)";
					totals[j] += appsByCriticality[3-i][j];
				} else {
					returnArray[i][j+1] = "0";
				}
			}
			returnArray[i][9] = String.valueOf(total);
			totals[8] += total;
			
		}
		
		returnArray[4][0] = "Total";
		
		for (int i = 0; i < 8; i++) {
			if (totals[i] != 0) {
				returnArray[4][i+1] = String.valueOf(totals[i]) + 
											" (" + 
											String.valueOf(100*totals[i]/totals[8]) +
											"%)";
			} else {
				returnArray[4][i+1] = "0";
			}
		}
		
		returnArray[4][9] = String.valueOf(totals[8]);
		
		return returnArray;
	}
	
	/**
	 * Update the app counts by criticality so that we can use those stats in the report.
	 * @param app
	 */
	private void updateCriticalityCounts(int[][] appsByCriticality, Application app, Calendar now) {

		if (app != null && app.getApplicationCriticality() != null &&
				app.getApplicationCriticality().getName() != null) {
			
			int arrayIndex = ApplicationCriticality.NUMERIC_MAP.get(
					app.getApplicationCriticality().getName());
			
			appsByCriticality[arrayIndex][8] += 1;

			if (app.getScans() == null || app.getScans().isEmpty()) {
				appsByCriticality[arrayIndex][7] += 1;
			} else {
				int numMonths = getAgeInMonths(now, app.getScans().get(0).getImportTime());
				if (numMonths == 0) {
					appsByCriticality[arrayIndex][0] += 1;
				} else {
					int index = 0;

					if (numMonths < 4) {
						index = numMonths-1;
					} else if (numMonths < 7) {
						index = 3;
					} else if (numMonths < 10) {
						index = 4;
					} else if (numMonths < 13) {
						index = 5;
					} else {
						index = 6;
					}
					
					appsByCriticality[arrayIndex][index] += 1;
				}
			}
		}
	}
	
	/**
	 * @param now
	 * @param older
	 * @return number of days difference between the older calendar and the newer one.
	 */
	private int getAgeInDays(Calendar now, Calendar older) {
		int numDays = 0;
		
		Date temp = older.getTime();
		
		while (older.before(now)) {
			numDays += 1;
			older.add(Calendar.DATE, 1);
		}
		
		older.setTime(temp);
		
		return numDays;
	}
	
	/**
	 * @param now
	 * @param older
	 * @return number of days difference between the older calendar and the newer one.
	 */
	private int getAgeInMonths(Calendar now, Calendar older) {
		int numMonths = 0;
		
		Date temp = older.getTime();
		
		while (older.before(now)) {
			numMonths += 1;
			older.add(Calendar.MONTH, 1);
		}
		
		older.setTime(temp);
		
		return numMonths;
	}
	
}