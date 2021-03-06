

ASSETS = anhang/assets
PARTIALS = anhang/partials
USERSTORIES = userstories

all: dirs qs-dokument.pdf userstories.pdf projekttagebuch.pdf

clean:
	rm -rf $(ASSETS) $(PARTIALS)

deleteallaux:
	find . -name '*.aux' -exec 'rm' '{}' ';'

dirs:
	mkdir -p $(ASSETS) $(PARTIALS) $(USERSTORIES)


$(PARTIALS)/codereviews.tex: belege/code-reviews/*.md
	cat belege/code-reviews/*.md | pandoc -r markdown -w latex | sed 's/\\subsection/\\clearpage\\subsection/' > $@

$(PARTIALS)/manuelle-tests-ui-reports.tex: belege/manuelle-tests/ui/*.md
	cat $^ | pandoc -r markdown -w latex | sed 's/\\subsection/\\clearpage\\subsection/' > $@

#$(PARTIALS)/grosstests.tex: belege/grosstests/*.md
#	cat $^ | pandoc -r markdown -w latex | sed 's/\\subsection/\\clearpage\\subsection/' > $@

$(PARTIALS)/dokumentation.tex: ../docs/Dokumentation.md
	pandoc -r markdown -w latex -o $@ $<

$(PARTIALS)/coverage.pdf: belege/coverage/index.html
	wkhtmltopdf $< $@

belege/junit/javadoc.html: belege/junit/javadoc/*
	cd toolchain && ./make-javadoc-html.sh

$(PARTIALS)/javadoc.pdf: belege/junit/javadoc.html
	wkhtmltopdf $< $@

$(PARTIALS)/junit.pdf: belege/junit/bonfire-junit-report.html
	wkhtmltopdf $< $@

$(PARTIALS)/checkstyle%.pdf: belege/checkstyle/checkstyle%.xml.html
	cd toolchain && ./make-checkstyle-html.sh
	wkhtmltopdf --lowquality $< $@

$(PARTIALS)/lint-results%.pdf: belege/lint/lint-results%.tmp.html
	wkhtmltopdf --lowquality $< $@

belege/lint/lint-results%.tmp.html: belege/lint/lint-results%.html
	cat $< > $@
	echo "<style> body,pre {background: white; } body,*{ color: black} body a {color:#00a!important} </style>" >> $@

$(PARTIALS)/findbugs%.pdf: belege/findbugs/findbugs%.html
	wkhtmltopdf --lowquality $< $@

$(USERSTORIES)/userstories.tex:
	toolchain/userstories.py > $@

qs-dokument.pdf: qs-dokument.tex anhang/*.tex spezifikation/*.tex \
		$(PARTIALS)/codereviews.tex $(PARTIALS)/manuelle-tests-ui-reports.tex $(PARTIALS)/dokumentation.tex \
		$(PARTIALS)/coverage.pdf $(PARTIALS)/javadoc.pdf $(PARTIALS)/junit.pdf $(PARTIALS)/checkstyle-1.pdf \
		$(PARTIALS)/checkstyle-2.pdf $(PARTIALS)/lint-results-1.pdf $(PARTIALS)/lint-results-2.pdf \
		$(PARTIALS)/findbugs-1.pdf $(PARTIALS)/findbugs-2.pdf
	pdflatex qs-dokument.tex
	pdflatex qs-dokument.tex

userstories.pdf: userstories.tex $(USERSTORIES)/userstories.tex
	pdflatex userstories.tex
	pdflatex userstories.tex

projekttagebuch.pdf: projekttagebuch.tex
	pdflatex projekttagebuch.tex
	pdflatex projekttagebuch.tex

open:
	xdg-open qs-dokument.pdf

rebuild: clean all

.PHONY: dirs clean $(USERSTORIES)/userstories.tex
