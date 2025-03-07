from bs4 import BeautifulSoup 
import time 
import requests 
from random import randint 
from html.parser import HTMLParser 
import json
USER_AGENT = {'User-Agent':'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36'} 
class SearchEngine: 
    @staticmethod 
    def search(query, sleep=True): 
        if sleep: # Prevents loading too many pages too soon 
            time.sleep(randint(4, 30)) 
            temp_url = '+'.join(query.split()) #for adding + between words for the query 
        url = 'http://www.bing.com/search?q=' + temp_url +'&count=30' # Limit to top 10 results
        soup = BeautifulSoup(requests.get(url, headers=USER_AGENT).text, "html.parser")
        new_results = SearchEngine.scrape_search_result(soup)
        return new_results # Return only first 10 results
    @staticmethod
    def scrape_search_result(soup):
        raw_results = soup.find_all("li", attrs={"class": "b_algo"})
        results = []
        links_seen = set() 
        if len(raw_results) <10:
            print(len(raw_results))
        for result in raw_results:
            link_element = result.find('a', href=True)
            if link_element:
                link = link_element['href']
                if link not in links_seen:
                    results.append(link)
                    links_seen.add(link)
                    if len(results) == 10:
                        break
        return results
if __name__ == "__main__":
    results_dict = {}
    print("Starting search...")
    with open('100queries.txt', 'r') as f:
        search_queries = f.read().splitlines()
        for i, search_query in enumerate(search_queries):
            search_query = search_query.strip()
            print(f"Processing query {i+1} of {len(search_queries)}: {search_query}")
            results = SearchEngine.search(search_query)
            results_dict[search_query] = results

    
    with open('search_results.json', 'w') as f:
        json.dump(results_dict, f, indent=4)
    