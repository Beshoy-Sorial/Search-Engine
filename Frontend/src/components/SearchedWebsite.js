class SearchedWebsite {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.searchResults = [];
        this.loading = false;
        this.error = null;
    }

    async search(query) {
        if (!query) return;
        
        this.showLoading();
        
        try {
            const response = await fetch(`http://localhost:8080/Search?query=${encodeURIComponent(query)}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            this.searchResults = data;
            this.render();
        } catch (err) {
            this.showError(err.message);
            console.error('Search error:', err);
        }
    }

    showLoading() {
        const resultsContainer = document.getElementById('searchResults');
        resultsContainer.innerHTML = `
            <div class="loading">
                Searching...
                <div class="loading-bar-container">
                    <div class="loading-bar"></div>
                </div>
            </div>
        `;
    }

    showError(message) {
        const resultsContainer = document.getElementById('searchResults');
        resultsContainer.innerHTML = `<div class="error">Error fetching search results: ${message}</div>`;
    }

    render() {
        const resultsContainer = document.getElementById('searchResults');
        
        if (this.searchResults.length === 0) {
            resultsContainer.innerHTML = '<div class="no-results">No results found</div>';
            return;
        }

        const resultsHtml = this.searchResults.map(result => `
            <div class="Site">
                <div class="title">
                    <a href="${result.url}" target="_blank" rel="noopener noreferrer">
                        ${result.title}
                    </a>
                </div>
                <div class="url">${result.url}</div>
                <div class="snippet">${result.snippet}</div>
                <div class="score">Relevance Score: ${result.score.toFixed(2)}</div>
            </div>
        `).join('');

        resultsContainer.innerHTML = resultsHtml;
    }
}

export default SearchedWebsite; 