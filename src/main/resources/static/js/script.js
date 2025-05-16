document.addEventListener('DOMContentLoaded', () => {
  const crawlerForm = document.getElementById('crawler-form');
  const crawlersTableBody = document.querySelector('#crawlers-table tbody');
  const logsModal = document.getElementById('log-modal');
  const logsContent = document.getElementById('log-content');
  const closeModal = document.querySelector('.close-button');
  const noCrawlersMessage = document.getElementById('no-crawlers-message');

  // 추가된 UI 요소 참조
  const notificationTypeSelect = document.getElementById('notification-type');
  const slackChannelIdGroup = document.getElementById('slack-channel-id-group');
  const slackChannelIdInput = document.getElementById('slack-channel-id');
  const emailInput = document.getElementById('email'); // 이메일 필드 참조

  let editingCrawlerId = null;

  // 초기 크롤러 목록 로드
  fetchCrawlers();

  // 알림 유형 변경 시 Slack 채널 ID 필드 표시/숨김 처리
  notificationTypeSelect.addEventListener('change', () => {
    const selectedType = notificationTypeSelect.value;
    if (selectedType === 'SLACK' || selectedType === 'BOTH') {
      slackChannelIdGroup.style.display = 'block';
    } else {
      slackChannelIdGroup.style.display = 'none';
    }
    // 이메일 필드의 required 속성 동적 제어 (선택적)
    emailInput.required = (selectedType === 'EMAIL' || selectedType === 'BOTH');
  });

  // 폼 제출 이벤트
  crawlerForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const notificationType = notificationTypeSelect.value;
    const email = emailInput.value;
    const slackChannelId = slackChannelIdInput.value;

    // 프론트엔드 유효성 검사 (간단하게)
    if (notificationType === 'EMAIL' || notificationType === 'BOTH') {
      if (!email) {
        showMessage('알림 유형이 EMAIL 또는 BOTH일 경우 이메일은 필수입니다.', 'error');
        return;
      }
    }
    if (notificationType === 'SLACK' || notificationType === 'BOTH') {
      if (!slackChannelId) {
        showMessage('알림 유형이 SLACK 또는 BOTH일 경우 Slack 채널 ID는 필수입니다.', 'error');
        return;
      }
    }

    const crawlerData = {
      url: document.getElementById('url').value,
      selector: document.getElementById('selector').value,
      checkInterval: parseInt(document.getElementById('check-interval').value, 10),
      alertKeyword: document.getElementById('alert-keyword').value,
      alertOnChange: document.getElementById('alert-on-change').checked,
      email: email, // email 값 사용
      notificationType: notificationType, // 알림 유형 추가
      slackChannelId: slackChannelId, // Slack 채널 ID 추가
    };

    try {
      let response;
      const method = editingCrawlerId ? 'PUT' : 'POST';
      const url = editingCrawlerId ? `/api/crawlers/${editingCrawlerId}` : '/api/crawlers';

      response = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(crawlerData),
      });

      if (!response.ok) {
        const errorResult = await response.json();
        throw new Error(errorResult.message || `HTTP error! status: ${response.status}`);
      }
      
      showMessage(editingCrawlerId ? '크롤러가 성공적으로 수정되었습니다.' : '크롤러가 성공적으로 생성되었습니다.', 'success');
      fetchCrawlers();
      resetForm(); // 폼 리셋 함수 호출

    } catch (error) {
      console.error('Error saving crawler:', error);
      showMessage(`크롤러 저장 실패: ${error.message}`, 'error');
    }
  });

  function resetForm() {
    crawlerForm.reset();
    editingCrawlerId = null;
    document.querySelector('#crawler-config-section h2').textContent = '크롤러 설정'; // 제목 변경
    document.querySelector('button[type="submit"]').textContent = '새 크롤러 등록하기'; // 버튼 텍스트 변경
    
    // 알림 유형 및 Slack 채널 ID 필드 초기화
    notificationTypeSelect.value = 'EMAIL'; // 기본값으로 설정
    slackChannelIdGroup.style.display = 'none'; // Slack 채널 ID 필드 숨김
    slackChannelIdInput.value = '';
    emailInput.required = true; // 이메일 필드 다시 필수로 (기본 EMAIL 타입)
    
    // 수정 취소 버튼 숨김 (만약 있다면)
    const cancelEditButton = document.getElementById('cancel-edit-button');
    if (cancelEditButton) {
        cancelEditButton.style.display = 'none';
    }
  }
  
  // 수정 취소 버튼 이벤트 (만약 있다면, index.html에 <button type="button" id="cancel-edit-button" style="display:none;">취소</button> 추가 필요)
  const cancelEditButton = document.getElementById('cancel-edit-button');
  if (cancelEditButton) {
    cancelEditButton.addEventListener('click', () => {
        resetForm();
    });
  }

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
    crawlersTableBody.innerHTML = ''; 

    if (crawlers.length === 0) {
      if (noCrawlersMessage) noCrawlersMessage.style.display = 'block';
      // 테이블 컬럼 개수가 늘어났으므로 colspan 수정
      crawlersTableBody.innerHTML = '<tr><td colspan="10">등록된 크롤러가 없습니다.</td></tr>'; 
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
      
      let notificationTypeText = crawler.notificationType;
      if (crawler.notificationType === 'EMAIL') notificationTypeText = '이메일';
      else if (crawler.notificationType === 'SLACK') notificationTypeText = 'Slack';
      else if (crawler.notificationType === 'BOTH') notificationTypeText = '이메일+Slack';


      tr.innerHTML = `
        <td>${index + 1}</td>
        <td title="${crawler.url}">${crawler.url}</td>
        <td title="${crawler.selector}">${crawler.selector}</td>
        <td>${Math.floor(crawler.checkInterval / 60000)} 분</td>
        <td title="${alertConditionText}">${alertConditionText}</td>
        <td>${notificationTypeText}</td> <!-- 알림 유형 표시 -->
        <td title="${crawler.slackChannelId || '-'}">${crawler.slackChannelId || '-'}</td> <!-- Slack 채널 ID 표시 -->
        <td><span class="status-${crawler.status.toLowerCase()}">${statusText}</span></td>
        <td title="${crawler.lastCrawledValue || '-'}">${crawler.lastCrawledValue || '-'}</td>
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
    emailInput.value = crawler.email || ''; // email 설정
    
    // 알림 유형 및 Slack 채널 ID 설정
    notificationTypeSelect.value = crawler.notificationType;
    slackChannelIdInput.value = crawler.slackChannelId || '';

    // 알림 유형에 따라 Slack 채널 ID 필드 표시/숨김 및 이메일 필수 여부 설정
    if (crawler.notificationType === 'SLACK' || crawler.notificationType === 'BOTH') {
      slackChannelIdGroup.style.display = 'block';
    } else {
      slackChannelIdGroup.style.display = 'none';
    }
    emailInput.required = (crawler.notificationType === 'EMAIL' || crawler.notificationType === 'BOTH');


    editingCrawlerId = crawler.id;
    document.querySelector('#crawler-config-section h2').textContent = '크롤러 수정';
    document.querySelector('button[type="submit"]').textContent = '수정 완료';
    
    // 수정 취소 버튼 표시
    const cancelEditButton = document.getElementById('cancel-edit-button');
    if (cancelEditButton) {
        cancelEditButton.style.display = 'inline-block'; // 또는 'block'
    }
    window.scrollTo(0, 0);
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
      fetchCrawlers(); 
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
      fetchCrawlers(); 
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
      fetchCrawlers();
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
    // 실제 사용자에게는 alert보다 좀 더 나은 UI의 알림 컴포넌트를 사용하는 것이 좋습니다.
    // 예: Toast 메시지, 모달 내 메시지 영역 등
    alert(`${type.toUpperCase()}: ${message}`);
  }
});