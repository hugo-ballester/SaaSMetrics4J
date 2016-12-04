SaaSMetrics4J
=============

The SaaS revenue model, based on recurrent revenue and sales expansion, is a simple, intuitive and powerful model and an ally for young startups. It provides clear KPIs for growth and forces you to concentrate on what matters: sustainable growth.

However computing the actual SaaS KPIs of your business, and generally keeping track of what really matters, can be hard. You'll need to do a LOT of NASTY Excel before obtaining those KIPs, and you'll live in fear of error. Examples of things that get messy:
- computing MRR, espansion and churn, independently of your billing schemes,
- keeping track of new and cancelled contracts, as well as changes in the existing ones, 
- dealing with different starting and ending contract times
- dealing with projects of fixed duration with set-up fees
- and a few other nasty things that will burn your late night hours...

After fighting with Excel for a year or so and not finding any free tools that suited me, I ended up developing a simple Java library to compute the things I need to know every month:
- bills with multiple items (one client with many open contracts)
- statistics per client, per contract, per period
- simple billing reports
- Monthly SasS KPIs independent of billing (MRR, churn, expansion, etc.)

I hope this library can be of help for other geeks out there trying to keep control of the exponential growth of their companies ;)

Hugo Zaragoza, hugo.zaragoza@websays.com, hugo@hugo-zaragoza.net.
