/*
 * @Author: your name
 * @Date: 2020-06-09 10:15:15
 * @LastEditTime: 2020-06-14 20:06:43
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: \OPReactViaGit\src\FetchLatestNews.js
 */ 

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import { withStyles } from '@material-ui/core/styles';
 import FormGroup from '@material-ui/core/FormGroup';
 import Switch from '@material-ui/core/Switch';
 import Grid from '@material-ui/core/Grid';
 import Typography from '@material-ui/core/Typography';

 export default function FetchLatestNews() {
  var [data, setData] = useState([]);
  const [query, setQuery] = useState("releases");
  const [skip, setSkip] = useState(1);
  const [showText, setShowText] = useState(false);
  const [state, setState] = React.useState({
    checkedC: false,
  });
  const handleChange = (event) => {
    setState({ ...state, [event.target.name]: event.target.checked });
    setShowText(!showText)
  };

  //default with nothing..
  const [url, setUrl] = useState(
    'https://news.api.gov.bc.ca/api/Posts/Latest/home/default?postKind=releases&count=1&skip=0&api-version=1.0',
  );
 
  //??G
  useEffect(() => {
    const fetchData = async () => {
      const result = await axios(url);
      setData(result.data);

    };
 
    fetchData();
  }, [url]);
 
  return (
    <React.Fragment>
      <input
        type="text"
        value={query}
        onChange={event => setQuery(event.target.value)}
      />
      <button
        type="button"
        onClick={() =>
          setUrl(`https://news.api.gov.bc.ca/api/Posts/Latest/home/default?postKind=${query}&count=1&skip=0&api-version=1.0`)
        }
      >
        Search
      </button>
      <p>Search category: releases, stories, factsheets, updates or default</p>

      <ul>
        {data.map(item =>  (
          <li key={item.atomId}>
            {item.documents.map(documents => <h4 key = {documents.languageId}>{documents.headline} </h4>)}
            <b> news type:</b>  {item.kind} <br/>
            <b> news key:</b>  {item.key} <br/>
            
            <FormGroup>
              <Typography component="div">
                <Grid component="label" container alignItems="center" spacing={1}>
                  <Grid item>Hide</Grid>
                  <Grid item>
                    <AntSwitch checked={state.checkedC} onChange={handleChange} name="checkedC" />
                  </Grid>
                  <Grid item>Show</Grid>
                </Grid>
              </Typography>
            </FormGroup>
            {/*//reverse ASCII code from api ..*/} 
            {showText && item.documents.map(documents => <p key = {documents.languageId}>{documents.detailsHtml = documents.detailsHtml.replace(/(<([^>]+)>)/ig, '')
                                                                                                                                             .replace(/&rsquo;/ig, '\'')
                                                                                                                                             .replace(/(&ldquo;)|(&rdquo;)/g, '"')
                                                                                                                                             .replace(/&ndash;/ig, ' - ')
                                                                                                                                             .replace(/&lsquo;/, '\'')
                                                                                                                                             .replace(/&nbsp;/ig, ' ')
                                                                                                                                             }</p>)}
          </li> 
        ))}
      </ul>
      
      <button
          type="button"
          onClick={() => {
            setSkip(skip + 1);
            setUrl(`https://news.api.gov.bc.ca/api/Posts/Latest/home/default?postKind=${query}&count=1&skip=${skip}&api-version=1.0`); 
          }
          }
        >
          Next
      </button>
    </React.Fragment>
  );
}

const AntSwitch = withStyles((theme) => ({
  root: {
    width: 28,
    height: 16,
    padding: 0,
    display: 'flex',
  },
  switchBase: {
    padding: 2,
    color: theme.palette.grey[500],
    '&$checked': {
      transform: 'translateX(12px)',
      color: theme.palette.common.white,
      '& + $track': {
        opacity: 1,
        backgroundColor: theme.palette.primary.main,
        borderColor: theme.palette.primary.main,
      },
    },
  },
  thumb: {
    width: 12,
    height: 12,
    boxShadow: 'none',
  },
  track: {
    border: `1px solid ${theme.palette.grey[500]}`,
    borderRadius: 16 / 2,
    opacity: 1,
    backgroundColor: theme.palette.common.white,
  },
  checked: {},
}))(Switch);
