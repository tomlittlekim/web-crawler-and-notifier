document.addEventListener('DOMContentLoaded', () => {
    const loadingElement = document.getElementById('loading');
    const dashboardContainer = document.getElementById('dashboard-container');

    const overallStatsContent = document.getElementById('overall-stats-content');
    const eventTypeSummaryTable = document.getElementById('event-type-summary-table');
    const eventTypeChartCanvas = document.getElementById('eventTypeChart').getContext('2d');
    const recentErrorsContent = document.getElementById('recent-errors-content');
    const urlStatsContent = document.getElementById('url-stats-content');

    let eventTypeChart = null; // 차트 객체 저장 변수

    async function fetchData(url) {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error(`Error fetching ${url}:`, error);
            return null; // 오류 발생 시 null 반환
        }
    }

    function renderOverallStats(data) {
        if (!data) {
            overallStatsContent.innerHTML = '<p>데이터를 불러오지 못했습니다.</p>';
            return;
        }
        overallStatsContent.innerHTML = `
            <table>
                <tr><th>총 시도</th><td>${data.totalAttempts}</td></tr>
                <tr><th>성공</th><td class="success">${data.successCount}</td></tr>
                <tr><th>실패</th><td class="failure">${data.failureCount}</td></tr>
                <tr><th>성공률</th><td class="percentage">${data.successRate.toFixed(2)}%</td></tr>
            </table>
        `;
    }

    function renderEventTypeSummary(data) {
        if (!data || data.length === 0) {
            eventTypeSummaryTable.innerHTML = '<p>데이터가 없습니다.</p>';
            if(eventTypeChart) eventTypeChart.destroy(); // 기존 차트 파괴
            return;
        }

        let tableHtml = '<table><tr><th>이벤트 유형</th><th>총 시도</th><th>성공</th><th>실패</th><th>성공률</th></tr>';
        const labels = [];
        const successData = [];
        const failureData = [];

        data.forEach(item => {
            tableHtml += `
                <tr>
                    <td>${item.eventType}</td>
                    <td>${item.totalAttempts}</td>
                    <td class="success">${item.successCount}</td>
                    <td class="failure">${item.failureCount}</td>
                    <td class="percentage">${item.successRate.toFixed(2)}%</td>
                </tr>
            `;
            labels.push(item.eventType);
            successData.push(item.successCount);
            failureData.push(item.failureCount);
        });
        tableHtml += '</table>';
        eventTypeSummaryTable.innerHTML = tableHtml;

        if(eventTypeChart) {
            eventTypeChart.destroy();
        }
        eventTypeChart = new Chart(eventTypeChartCanvas, {
            type: 'bar', // 또는 'pie'
            data: {
                labels: labels,
                datasets: [
                    {
                        label: '성공',
                        data: successData,
                        backgroundColor: 'rgba(75, 192, 192, 0.7)',
                        borderColor: 'rgba(75, 192, 192, 1)',
                        borderWidth: 1
                    },
                    {
                        label: '실패',
                        data: failureData,
                        backgroundColor: 'rgba(255, 99, 132, 0.7)',
                        borderColor: 'rgba(255, 99, 132, 1)',
                        borderWidth: 1
                    }
                ]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    function renderRecentErrors(data) {
        if (!data || data.length === 0) {
            recentErrorsContent.innerHTML = '<p>최근 오류가 없습니다.</p>';
            return;
        }
        let tableHtml = '<table><tr><th>시간</th><th>유형</th><th>대상 URL</th><th>상세</th></tr>';
        data.forEach(item => {
            tableHtml += `
                <tr>
                    <td>${new Date(item.eventTimestamp).toLocaleString()}</td>
                    <td>${item.eventType}</td>
                    <td title="${item.targetUrl || ''}">${item.targetUrl || '-'}</td>
                    <td title="${item.details || ''}"><div class="error-details">${item.details || '-'}</div></td>
                </tr>
            `;
        });
        tableHtml += '</table>';
        recentErrorsContent.innerHTML = tableHtml;
    }

    function renderUrlStats(data) {
        if (!data || data.length === 0) {
            urlStatsContent.innerHTML = '<p>URL별 통계 데이터가 없습니다.</p>';
            return;
        }
        let tableHtml = '<table><tr><th>URL</th><th>시도</th><th>성공</th><th>실패</th><th>성공률</th><th>평균 성공 시간(ms)</th><th>최근 실패 시간</th><th>최근 확인</th></tr>';
        data.forEach(item => {
            tableHtml += `
                <tr>
                    <td title="${item.targetUrl}">${item.targetUrl}</td>
                    <td>${item.totalAttempts}</td>
                    <td class="success">${item.successCount}</td>
                    <td class="failure">${item.failureCount}</td>
                    <td class="percentage">${item.successRate.toFixed(2)}%</td>
                    <td>${item.averageDurationMsSuccess ? item.averageDurationMsSuccess.toFixed(0) : '-'}</td>
                    <td title="${item.lastFailureTimestamp ? new Date(item.lastFailureTimestamp).toLocaleString() : ''}">${item.lastFailureTimestamp ? new Date(item.lastFailureTimestamp).toLocaleString() : '-'}</td>
                    <td>${item.lastCheckedAt ? new Date(item.lastCheckedAt).toLocaleString() : '-'}</td>
                </tr>
            `;
        });
        tableHtml += '</table>';
        urlStatsContent.innerHTML = tableHtml;
    }


    async function fetchAndRenderAll() {
        loadingElement.style.display = 'block';
        dashboardContainer.style.display = 'none';

        const overallStats = await fetchData('/api/statistics/overall-crawl');
        renderOverallStats(overallStats);

        const eventTypeSummary = await fetchData('/api/statistics/event-type-summary');
        renderEventTypeSummary(eventTypeSummary);

        const recentErrors = await fetchData('/api/statistics/recent-errors?limit=10');
        renderRecentErrors(recentErrors);

        const urlStats = await fetchData('/api/statistics/url-crawl-stats');
        renderUrlStats(urlStats);

        loadingElement.style.display = 'none';
        dashboardContainer.style.display = 'grid'; // HTML에서 grid로 설정했으므로
    }

    fetchAndRenderAll();
}); 