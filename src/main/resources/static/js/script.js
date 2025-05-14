document.addEventListener('DOMContentLoaded', () => {
  const crawlerForm = document.getElementById('crawler-form');
  const crawlersTableBody = document.querySelector('#crawlers-table tbody');
  const noCrawlersMessage = document.getElementById('no-crawlers-message');
  const submitButton = document.getElementById('submit-button');
  const cancelEditButton = document.getElementById('cancel-edit-button');
  const crawlerIdInput = document.getElementById('crawler-id');

  const logModal = document.getElementById('log-modal');
  const logContent = document.getElementById('log-content');
  const closeModalButton = document.querySelector('.modal .close-button');

  const API_BASE_URL = '/api/crawlers'; // Spring Boot 기본 API 경로

  // 크롤러 목록 불러오기
  async function fetchCrawlers() {
    try {
      const response = await fetch(API_BASE_URL);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const crawlers = await response.json();
      renderCrawlers(crawlers);
    } catch (error) {
      console.error('Error fetching crawlers:', error);
      crawlersTableBody.innerHTML = `<tr><td colspan="8">크롤러 목록을 불러오는 중 오류가 발생했습니다: ${error.message}</td></tr>`;
      noCrawlersMessage.style.display = 'none';
    }
  }

  // 크롤러 목록 렌더링
  function renderCrawlers(crawlers) {
    crawlersTableBody.innerHTML = ''; // 기존 목록 초기화
    if (crawlers && crawlers.length > 0) {
      noCrawlersMessage.style.display = 'none';
      crawlers.forEach((crawler, index) => {
        const row = crawlersTableBody.insertRow();
        row.innerHTML = `
                    <td>${index + 1}</td>
                    <td title="${crawler.url}">${truncateText(crawler.url, 30)}</td>
                    <td>${crawler.selector}</td>
                    <td>${formatInterval(crawler.checkInterval)}</td>
                    <td>${getAlertConditionText(crawler)}</td>
                    <td>${crawler.status || 'N/A'} <small>(${crawler.lastCheckedAt ? new Date(crawler.lastCheckedAt).toLocaleString() : '아직 확인 안됨'})</small></td>
                    <td title="${crawler.lastCrawledValue || ''}">${truncateText(crawler.lastCrawledValue || 'N/A', 20)}</td>
                    <td>
                        <button class="action-button-edit" data-id="${crawler.id}">수정</button>
                        <button class="action-button-delete" data-id="${crawler.id}">삭제</button>
                        <button class="action-button-check" data-id="${crawler.id}">즉시확인</button>
                        <button class="action-button-log" data-id="${crawler.id}">로그</button>
                    </td>
                `;
      });
    } else {
      noCrawlersMessage.style.display = 'block';
    }
    addEventListenersToButtons();
  }

  function truncateText(text, maxLength) {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }

  function formatInterval(ms) {
    if (ms === 86400000) return '매일';
    if (ms === 3600000) return '1시간마다';
    if (ms === 1800000) return '30분마다';
    if (ms === 600000) return '10분마다';
    if (ms === 300000) return '5분마다';
    return `${ms / 1000}초마다`;
  }

  function getAlertConditionText(crawler) {
    const conditions = [];
    if (crawler.alertKeyword) {
      conditions.push(`키워드: "${crawler.alertKeyword}"`);
    }
    if (crawler.alertOnChange) {
      conditions.push("내용 변경 시");
    }
    return conditions.length > 0 ? conditions.join(', ') : '설정 안됨';
  }


  // 폼 제출 (등록/수정)
  crawlerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const formData = new FormData(crawlerForm);
    const data = Object.fromEntries(formData.entries());
    data.checkInterval = parseInt(data.checkInterval, 10); // 숫자로 변환
    data.alertOnChange = formData.has('alertOnChange'); // 체크박스 값 처리

    const id = data.id;
    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE_URL}/${id}` : API_BASE_URL;

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message || response.statusText}`);
      }

      fetchCrawlers(); // 목록 새로고침
      crawlerForm.reset();
      crawlerIdInput.value = '';
      submitButton.textContent = '새 크롤러 등록하기';
      cancelEditButton.style.display = 'none';
      alert(id ? '크롤러가 수정되었습니다.' : '크롤러가 등록되었습니다.');

    } catch (error) {
      console.error('Error submitting form:', error);
      alert(`오류가 발생했습니다: ${error.message}`);
    }
  });

  // 수정 모드 취소
  cancelEditButton.addEventListener('click', () => {
    crawlerForm.reset();
    crawlerIdInput.value = '';
    submitButton.textContent = '새 크롤러 등록하기';
    cancelEditButton.style.display = 'none';
  });


  // 액션 버튼들에 이벤트 리스너 추가
  function addEventListenersToButtons() {
    document.querySelectorAll('.action-button-edit').forEach(button => {
      button.addEventListener('click', handleEdit);
    });
    document.querySelectorAll('.action-button-delete').forEach(button => {
      button.addEventListener('click', handleDelete);
    });
    document.querySelectorAll('.action-button-check').forEach(button => {
      button.addEventListener('click', handleCheckNow);
    });
    document.querySelectorAll('.action-button-log').forEach(button => {
      button.addEventListener('click', handleShowLog);
    });
  }

  // 수정 버튼 클릭 시
  async function handleEdit(event) {
    const id = event.target.dataset.id;
    try {
      const response = await fetch(`${API_BASE_URL}/${id}`);
      if (!response.ok) throw new Error('Failed to fetch crawler details for editing.');
      const crawler = await response.json();

      document.getElementById('crawler-id').value = crawler.id;
      document.getElementById('url').value = crawler.url;
      document.getElementById('selector').value = crawler.selector;
      document.getElementById('check-interval').value = crawler.checkInterval;
      document.getElementById('alert-keyword').value = crawler.alertKeyword || '';
      document.getElementById('alert-on-change').checked = crawler.alertOnChange || false;
      document.getElementById('email').value = crawler.email;

      submitButton.textContent = '설정 저장하기';
      cancelEditButton.style.display = 'inline-block';
      window.scrollTo(0, 0); // 페이지 상단으로 스크롤
    } catch (error) {
      console.error('Error loading crawler for edit:', error);
      alert('크롤러 정보를 불러오는 데 실패했습니다.');
    }
  }

  // 삭제 버튼 클릭 시
  async function handleDelete(event) {
    const id = event.target.dataset.id;
    if (confirm('정말로 이 크롤러를 삭제하시겠습니까?')) {
      try {
        const response = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE' });
        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
          throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message || response.statusText}`);
        }
        fetchCrawlers(); // 목록 새로고침
        alert('크롤러가 삭제되었습니다.');
      } catch (error) {
        console.error('Error deleting crawler:', error);
        alert(`크롤러 삭제 중 오류 발생: ${error.message}`);
      }
    }
  }

  // 즉시 확인 버튼 클릭 시
  async function handleCheckNow(event) {
    const id = event.target.dataset.id;
    try {
      const response = await fetch(`${API_BASE_URL}/${id}/check`, { method: 'POST' });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message || response.statusText}`);
      }
      const result = await response.json();
      alert(`즉시 확인 요청 완료. (결과: ${result.message || '성공'})`); // 백엔드 응답에 따라 메시지 조정
      fetchCrawlers(); // 목록 새로고침
    } catch (error) {
      console.error('Error checking crawler immediately:', error);
      alert(`즉시 확인 중 오류 발생: ${error.message}`);
    }
  }

  // 로그 보기 버튼 클릭 시
  async function handleShowLog(event) {
    const id = event.target.dataset.id;
    try {
      const response = await fetch(`${API_BASE_URL}/${id}/logs`);
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to fetch logs.' }));
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message || response.statusText}`);
      }

      const logs = await response.json();

      if (logs && logs.length > 0) {
        logContent.textContent = logs.map(log =>
            `[${new Date(log.crawledAt).toLocaleString()}] ${log.success ? '성공' : '실패'} - 값: ${log.crawledValue || 'N/A'}${log.errorMessage ? ` (에러: ${log.errorMessage})` : ''}${log.notificationSent ? ' - 알림 발송됨' : ''}`
        ).join('\n');
      } else {
        logContent.textContent = '해당 크롤러에 대한 로그가 없습니다.';
      }
      logModal.style.display = 'block';
    } catch (error) {
      console.error('Error fetching logs:', error);
      logContent.textContent = '로그를 불러오는 중 오류가 발생했습니다.';
      logModal.style.display = 'block';
    }
  }

  // 모달 닫기
  if(closeModalButton) {
    closeModalButton.onclick = function() {
      logModal.style.display = "none";
    }
  }
  window.onclick = function(event) {
    if (event.target == logModal) {
      logModal.style.display = "none";
    }
  }

  // 초기 데이터 로드
  fetchCrawlers();
});