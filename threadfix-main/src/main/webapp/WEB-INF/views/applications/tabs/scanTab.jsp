
<tab ng-controller="ScanTableController"
     heading="{{ heading }}">

    <table class="table table-striped">
        <thead>
            <tr>
                <th class="first">Channel</th>
                <th>Scan Date</th>
                <c:if test="${ not canUploadScans }">
                    <th>Total Vulns</th>
                </c:if>
                <c:if test="${ canUploadScans }">
                    <th style="text-align:center">Total Vulns</th>
                    <th style="text-align:center">Hidden Vulns</th>
                    <th class="medium"></th>
                </c:if>
                <th class="medium"></th>
            </tr>
        </thead>
        <tbody>
            <tr ng-hide="scans">
                <td colspan="6" style="text-align:center;">No scans found.</td>
            </tr>
            <tr ng-show="scans" ng-repeat="scan in scans" class="bodyRow">
                <td id="channelType{{ $index }}"> {{ scan.scannerName }} </td>
                <td>
                    {{ scan.importTime }}
                </td>
                <td style="text-align:center" id="numTotalVulnerabilities{{ $index }}">
                    {{ scan.numberTotalVulnerabilities }}
                </td>
                <td style="text-align:center" id="numHiddenVulnerabilities{{ $index }}">
                    {{ scan.numberHiddenVulnerabilities }}
                </td>
                <c:if test="${ canUploadScans }">
                    <td>
                        <a ng-click="deleteScan(scan)">Delete Scan</a>
                    </td>
                    <td>
                        <a ng-click="viewScan(scan)">View Scan</a>
                    </td>
                </c:if>
            </tr>
        </tbody>
    </table>

</tab>