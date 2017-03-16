function uriEncoding(template: TemplateStringsArray, ...substitutions: any[]): string {
	let result = "";
	for (let i = 0; i < substitutions.length; i++) {
		result += template[i];
		result += encodeURIComponent(substitutions[i]);
	}
	result += template[template.length - 1];
	return result;
}
