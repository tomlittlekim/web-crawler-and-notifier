document.addEventListener('DOMContentLoaded', () => {
  const crawlerForm = document.getElementById('crawler-form');
  const crawlersTableBody = document.querySelector('#crawlers-table tbody');
  const logsModal = document.getElementById('log-modal');
  const logsContent = document.getElementById('log-content');
  const closeModal = document.querySelector('.close-button');
  const noCrawlersMessage = document.getElementById('no-crawlers-message');

  let editingCrawlerId = null; // 수정 중인 크롤러의 ID

  // 초기 크롤러 목록 로드
  fetchCrawlers();

  crawlerForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const crawlerData = {
      url: document.getElementById('url').value,
      selector: document.getElementById('selector').value,
      checkInterval: parseInt(document.getElementById('check-interval').value, 10),
      alertKeyword: document.getElementById('alert-keyword').value,
      alertOnChange: document.getElementById('alert-on-change').checked,
      email: document.getElementById('email').value,
    };

    try {
      let response;
      if (editingCrawlerId) {
        // 수정 모드
        response = await fetch(`/api/crawlers/${editingCrawlerId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(crawlerData),
        });
      } else {
        // 생성 모드
        response = await fetch('/api/crawlers', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(crawlerData),
        });
      }

      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      showMessage(editingCrawlerId ? '크롤러가 성공적으로 수정되었습니다.' : '크롤러가 성공적으로 생성되었습니다.', 'success');
      fetchCrawlers();
      crawlerForm.reset();
      editingCrawlerId = null; // 수정 모드 해제
      document.querySelector('#crawler-config-section h2').textContent = '새 크롤러 등록';
      document.querySelector('button[type="submit"]').textContent = '등록';

    } catch (error) {
      console.error('Error saving crawler:', error);
      showMessage(`크롤러 저장 실패: ${error.message}`, 'error');
    }
  });

  closeModal.addEventListener('click', () => {
    logsModal.style.display = 'none';
  });

  window.addEventListener('click', (event) => {
    if (event.target === logsModal) {
      logsModal.style.display = 'none';
    }
  });

  async function fetchCrawlers() {
    try {
      const response = await fetch('/api/crawlers');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const crawlers = await response.json();
      renderCrawlers(crawlers);
    } catch (error) {
      console.error('Error fetching crawlers:', error);
      showMessage(`크롤러 목록 로드 실패: ${error.message}`, 'error');
    }
  }

  function renderCrawlers(crawlers) {
    crawlersTableBody.innerHTML = ''; // 기존 목록 초기화

    if (crawlers.length === 0) {
      if (noCrawlersMessage) noCrawlersMessage.style.display = 'block';
      crawlersTableBody.innerHTML = '<tr><td colspan="8">등록된 크롤러가 없습니다.</td></tr>'; // 테이블에 맞게 메시지 수정
      return;
    }

    if (noCrawlersMessage) noCrawlersMessage.style.display = 'none';

    crawlers.forEach((crawler, index) => {
      const tr = document.createElement('tr');
      tr.setAttribute('data-id', crawler.id);

      let statusText = crawler.status;
      if (crawler.status === 'ACTIVE') statusText = '활성';
      else if (crawler.status === 'INACTIVE') statusText = '비활성';
      else if (crawler.status === 'ERROR') statusText = '오류';

      // 알림 조건 텍스트 생성
      let alertConditionText = '';
      if (crawler.alertKeyword && crawler.alertOnChange) {
        alertConditionText = `키워드(${crawler.alertKeyword}) OR 변경 시`;
      } else if (crawler.alertKeyword) {
        alertConditionText = `키워드(${crawler.alertKeyword})`;
      } else if (crawler.alertOnChange) {
        alertConditionText = '변경 시';
      } else {
        alertConditionText = '-';
      }

      tr.innerHTML = `
        <td>${index + 1}</td>
        <td>${crawler.url}</td>
        <td>${crawler.selector}</td>
        <td>${Math.floor(crawler.checkInterval / 60000)} 분</td>
        <td>${alertConditionText}</td>
        <td><span class="status-${crawler.status.toLowerCase()}">${statusText}</span></td>
        <td>${crawler.lastCrawledValue || '-'}</td>
        <td class="action-buttons">
            <button class="edit-btn action-button-edit">수정</button>
            <button class="delete-btn action-button-delete">삭제</button>
            <button class="check-now-btn action-button-check">지금 확인</button>
            <button class="view-logs-btn action-button-log">로그 보기</button>
            ${ (crawler.status === 'INACTIVE' || crawler.status === 'ERROR')
              ? `<button class="activate-btn action-button-activate">활성화</button>`
              : '' }
            ${ (crawler.status === 'ACTIVE')
              ? `<button class="deactivate-btn action-button-deactivate">비활성화</button>`
              : '' }
        </td>
      `;

      tr.querySelector('.edit-btn').addEventListener('click', () => populateFormForEdit(crawler));
      tr.querySelector('.delete-btn').addEventListener('click', () => deleteCrawler(crawler.id));
      tr.querySelector('.check-now-btn').addEventListener('click', () => checkCrawlerNow(crawler.id));
      tr.querySelector('.view-logs-btn').addEventListener('click', () => viewLogs(crawler.id));

      const activateBtn = tr.querySelector('.activate-btn');
      if (activateBtn) {
        activateBtn.addEventListener('click', () => activateCrawler(crawler.id));
      }

      const deactivateBtn = tr.querySelector('.deactivate-btn');
      if (deactivateBtn) {
        deactivateBtn.addEventListener('click', () => deactivateCrawler(crawler.id));
      }

      crawlersTableBody.appendChild(tr);
    });
  }

  function populateFormForEdit(crawler) {
    document.getElementById('url').value = crawler.url;
    document.getElementById('selector').value = crawler.selector;
    document.getElementById('check-interval').value = crawler.checkInterval;
    document.getElementById('alert-keyword').value = crawler.alertKeyword || '';
    document.getElementById('alert-on-change').checked = crawler.alertOnChange;
    document.getElementById('email').value = crawler.email;

    editingCrawlerId = crawler.id;
    document.querySelector('#crawler-config-section h2').textContent = '크롤러 수정';
    document.querySelector('button[type="submit"]').textContent = '수정 완료';
    window.scrollTo(0, 0); // 폼으로 스크롤 이동
  }

  async function deleteCrawler(crawlerId) {
    if (!confirm('정말로 이 크롤러를 삭제하시겠습니까? 연관된 모든 로그도 함께 삭제됩니다.')) {
      return;
    }
    try {
      const response = await fetch(`/api/crawlers/${crawlerId}`, { method: 'DELETE' });
      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      showMessage('크롤러가 성공적으로 삭제되었습니다.', 'success');
      fetchCrawlers();
    } catch (error) {
      console.error('Error deleting crawler:', error);
      showMessage(`크롤러 삭제 실패: ${error.message}`, 'error');
    }
  }

  async function checkCrawlerNow(crawlerId) {
    try {
      const response = await fetch(`/api/crawlers/${crawlerId}/check`, { method: 'POST' });
      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      const result = await response.json();
      showMessage(result.message || '크롤러 즉시 확인 요청이 처리되었습니다.', 'success');
      fetchCrawlers(); // 상태 및 최근 확인 값 업데이트를 위해 목록 새로고침
    } catch (error) {
      console.error('Error checking crawler now:', error);
      showMessage(`크롤러 즉시 확인 실패: ${error.message}`, 'error');
    }
  }

  async function activateCrawler(crawlerId) {
    try {
      const response = await fetch(`/api/crawlers/${crawlerId}/activate`, { method: 'PUT' });
      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      showMessage('크롤러가 성공적으로 활성화되었습니다.', 'success');
      fetchCrawlers(); // 목록 새로고침하여 상태 반영
    } catch (error) {
      console.error('Error activating crawler:', error);
      showMessage(`크롤러 활성화 실패: ${error.message}`, 'error');
    }
  }

  async function deactivateCrawler(crawlerId) {
    try {
      const response = await fetch(`/api/crawlers/${crawlerId}/deactivate`, { method: 'PUT' });
      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      showMessage('크롤러가 성공적으로 비활성화되었습니다.', 'success');
      fetchCrawlers(); // 목록 새로고침하여 상태 반영
    } catch (error) {
      console.error('Error deactivating crawler:', error);
      showMessage(`크롤러 비활성화 실패: ${error.message}`, 'error');
    }
  }


  async function viewLogs(crawlerId) {
    try {
      const response = await fetch(`/api/crawlers/${crawlerId}/logs`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const logs = await response.json();
      renderLogs(logs, crawlerId);
    } catch (error) {
      console.error('Error fetching logs:', error);
      showMessage(`로그 로드 실패: ${error.message}`, 'error');
    }
  }

  function renderLogs(logs, crawlerId) {
    logsContent.innerHTML = `<h3>크롤러 ID: ${crawlerId} 로그</h3>`;
    if (logs.length === 0) {
      logsContent.innerHTML += '<p>기록된 로그가 없습니다.</p>';
      logsModal.style.display = 'block';
      return;
    }

    const table = document.createElement('table');
    table.innerHTML = `
            <thead>
                <tr>
                    <th>시간</th>
                    <th>값</th>
                    <th>성공</th>
                    <th>오류 메시지</th>
                    <th>알림 발송</th>
                </tr>
            </thead>
            <tbody>
                ${logs.map(log => `
                    <tr>
                        <td>${new Date(log.crawledAt).toLocaleString()}</td>
                        <td>${log.crawledValue || '-'}</td>
                        <td>${log.success ? '예' : '아니오'}</td>
                        <td>${log.errorMessage || '-'}</td>
                        <td>${log.notificationSent ? '예' : '아니오'}</td>
                    </tr>
                `).join('')}
            </tbody>
        `;
    logsContent.appendChild(table);
    logsModal.style.display = 'block';
  }

  function showMessage(message, type = 'info') {
    alert(`${type.toUpperCase()}: ${message}`);
  }
});