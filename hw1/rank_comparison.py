import json

def load_results(json_file):
    """Load JSON data from a file."""
    with open(json_file, 'r') as file:
        data = json.load(file)
    return data

def rank_urls(reference_urls, comparison_urls):
    """Rank URLs based on their presence in the reference URL list."""
    ranks = []
    for url in comparison_urls:
        # Append the rank if found, otherwise append None
        rank = reference_urls.index(url) + 1 if url in reference_urls else None
        ranks.append(rank)
    # Fill in ranks for missing URLs up to the number of reference URLs, if necessary
    while len(ranks) < len(reference_urls):
        ranks.append(None)
    return ranks

def calculate_spearman_coefficient(ranks1, ranks2):
    """Calculate the Spearman rank correlation coefficient between two lists of ranks."""
    # Ensure ranks lists are of equal length
    min_length = min(len(ranks1), len(ranks2))
    ranks1 = ranks1[:min_length]
    ranks2 = ranks2[:min_length]

    # Calculate the coefficient only on non-None rank pairs
    valid_pairs = [(r1, r2) for r1, r2 in zip(ranks1, ranks2) if r1 is not None and r2 is not None]
    n_valid = len(valid_pairs)
    
    if n_valid == 0:
        return 0,0  # Return 0 if no valid rank pairs exist

    if n_valid == 1:
        # Special case: only one valid pair
        return (1,1) if valid_pairs[0][0] == valid_pairs[0][1] else (0,1)

    d_squared_sum = sum((r1 - r2) ** 2 for r1, r2 in valid_pairs)
    rho = 1 - (6 * d_squared_sum) / (n_valid * (n_valid ** 2 - 1))
    return rho,n_valid


google_results = load_results('google_results.json')
other_engine_results = load_results('hw1.json')
#output to csv
#in the format: query, number of overlapping results, Percent Overlap, spearman coefficient
import csv
with open('hw1.csv', 'w', newline='') as csvfile:
    fieldnames = ['Queries', 'Number of Overlapping Results', 'Percent Overlap', 'Spearman Coefficient']
    #write header

    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    sum_spearman_coefficient = 0
    sum_percent_overlap = 0
    sum_overlapping_results = 0

    with open('100queries.txt', 'r') as f:
        search_queries = f.read().splitlines()
        for i,search_query in enumerate(search_queries):
            #remove last space from search query
            search_query = search_query[:-1]
            google_urls = google_results[search_query]
            other_engine_urls = other_engine_results[search_query]

            google_ranks = list(range(1, len(google_urls) + 1))
            other_engine_ranks = rank_urls(google_urls, other_engine_urls)

            spearman_coefficient,n_valid = calculate_spearman_coefficient(google_ranks, other_engine_ranks)
            #number of overlapping results
            number_of_overlapping_results = n_valid
            #percent overlap
            percent_overlap = (n_valid/len(google_urls))*100
            writer.writerow({'Queries':'Query'+str(i+1) , 'Number of Overlapping Results': number_of_overlapping_results, 'Percent Overlap': percent_overlap, 'Spearman Coefficient': spearman_coefficient})
            #calculate average after all queries
            sum_spearman_coefficient += spearman_coefficient
            sum_percent_overlap += percent_overlap
            sum_overlapping_results += number_of_overlapping_results
        average_spearman_coefficient = sum_spearman_coefficient/100
        average_percent_overlap = sum_percent_overlap/100
        average_overlapping_results = sum_overlapping_results/100
        writer.writerow({'Queries':'Average' , 'Number of Overlapping Results': average_overlapping_results, 'Percent Overlap': average_percent_overlap, 'Spearman Coefficient': average_spearman_coefficient})
        #average_percent_overlap
        
        #writer.writerow({'Queries':'Average' , 'Number of Overlapping Results': number_of_overlapping_results, 'Percent Overlap': average_percent_overlap, 'spearman coefficient