let SearchBar = document.querySelector(".SearchQuery");
const Spinner =  document.getElementById("loading");

Spinner.style.display = "none";

const query = sessionStorage.getItem("searchQuery");
const html = sessionStorage.getItem("searchResultsHTML");


sessionStorage.removeItem("searchQuery");
sessionStorage.removeItem("searchResultsHTML");


SearchBar.value = query;
if (html)
    document.querySelector("#searchResults").innerHTML = html;
else    
    document.querySelector("#searchResults").innerHTML = `<div class="no-results">NO Results were Found</div>`;


function performSearch() {
   
    let SearchedQuery = "";
    SearchedQuery = SearchBar.value;
    
    Spinner.style.display = "block";
    Spinner.style.color = "blue";


    console.log("Searching for: " + SearchedQuery);

    fetch("http://localhost:8080/Search?query=" + SearchedQuery)
        .then(response => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
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
                <div class="url">
                    ${result.url}
                </div>
                <div class="snippet">
                    ${result.snippet}
                </div>
                <div class="score">
                    ${result.score}
                </div>
            </div>
            `
            });
            Spinner.style.display = "none";
            document.querySelector("#searchResults").innerHTML = html;
            if (!html) {
                document.querySelector("#searchResults").innerHTML = `<div class="no-results">NO Results were Found</div>`;
            }
        })
        .catch(error => {
            console.error("Fetch error:", error);
            Spinner.style.display = "none";
        });
}
