interface GeocodeResponse {
    results: Array<{
        address_components: Array<{
            long_name: string;
            types: string[];
        }>;
    }>;
}

export async function getLocationFromCoordinates(lat: number, long: number) {
    const response = await fetch(`https://maps.googleapis.com/maps/api/geocode/json?latlng=${lat},${long}&key=${process.env.GEOCODING_API}`);
    const data = await response.json() as GeocodeResponse;
    if (data && data.results && data.results.length > 0) {
    const addressComponents = data.results[0].address_components;
    let country = '';
    let province = '';
    let city = '';
    for (const component of addressComponents) {
        if (component.types.includes('country')) {
            country = component.long_name;
        }
        if (component.types.includes('administrative_area_level_1')) {
            province = component.long_name;
        }
        if (component.types.includes('locality')) {
            city = component.long_name;
        }
    }
    return {
        country,
        province,
        city,
    };
}
else {
    throw new Error('No results found');
}
    
}