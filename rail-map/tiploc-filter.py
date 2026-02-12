import json

with open("tiplocs.json") as f:
    tiplocs = json.load(f)["Tiplocs"]

filtered_tiplocs = []

for tiploc in tiplocs:
    if tiploc['IsTiploc'] == False:
        continue
    if tiploc['Details']['OffNetwork'] == True:
        continue
    if "BUS" in tiploc['Codes']:
        continue
    
    filtered_tiplocs.append(tiploc)

with open("filtered_tiplocs.csv", "w") as f:
    f.write("tiploc,lat,lon\n")
    for tiploc in filtered_tiplocs:
        f.write(f"{tiploc['Tiploc']},{tiploc['Latitude']},{tiploc['Longitude']}\n")
