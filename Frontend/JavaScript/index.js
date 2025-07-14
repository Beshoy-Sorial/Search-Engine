const SearchBar = document.querySelector(".SearchQuery");
const Spinner =  document.getElementById("loading");

Spinner.style.display = "none";

function performSearchforIndex() {
    const SearchedQuery = SearchBar.value.trim();
    if (!SearchedQuery) return;

    Spinner.style.display = "block";
   

    console.log("Searching for: " + SearchedQuery);

    fetch("http://localhost:8080/Search?query=" + encodeURIComponent(SearchedQuery))
        .then(response => {
            if (!response.ok) throw new Error("Network response was not ok");
            return response.json();
        })
        .then(data => {
            const results = data.results || data;
            
          
            let html = "";

            results.forEach(result => {
                html += `
                <div class="Site">
                    <div class="title">
                        <a href="${result.url}" target="_blank">${result.title + " <=="}</a>
                    </div>
                    <div class="url">${result.url}</div>
                    <div class="snippet">${result.snippet}</div>
                    <div class="score">${result.score}</div>
                </div>`;
            });

            sessionStorage.setItem("searchQuery", SearchedQuery);
            sessionStorage.setItem("searchResultsHTML", html);
            window.location.href = "SearchedWebsites.html";
        })
        .catch(error => {
            console.error("Fetch error:", error);
            Spinner.style.display = "none";
        });
}
